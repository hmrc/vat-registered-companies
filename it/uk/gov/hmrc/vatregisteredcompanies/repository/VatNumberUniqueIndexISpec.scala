/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatregisteredcompanies.repository

import com.mongodb.MongoException
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.bson.{BsonBoolean, BsonDocument}
import org.mongodb.scala.model.{IndexOptions, Indexes}
import play.api.{Configuration, Environment, Mode}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, POST, route, running, status, writeableOf_AnyContentAsJson}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.vatregisteredcompanies.helpers.IntegrationSpecBase
import uk.gov.hmrc.vatregisteredcompanies.helpers.TestData.*
import uk.gov.hmrc.vatregisteredcompanies.models.Payload
import uk.gov.hmrc.vatregisteredcompanies.repositories.{DefaultLockRepository, PayloadBufferRepository, VatRegisteredCompaniesRepository, Wrapper}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import java.util.concurrent.{CyclicBarrier, Executors, TimeUnit}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class VatNumberUniqueIndexISpec extends IntegrationSpecBase {

  private val uniqueIndexName = "vatNumber_unique_idx"
  private val nonUniqueIndexName = "vatNumber_migration_non_unique_idx"
  private def collection = vatRegisteredCompaniesRepository.collection

  private def dropMigrationIndexes(): Unit =
    List(uniqueIndexName, nonUniqueIndexName).foreach { name =>
      await(collection.dropIndex(name).toFuture().recover {
        case e: MongoException if Set(26, 27).contains(e.getCode) => ()
      })
    }

  private def initialiseRepository(uniqueEnabled: Boolean): Future[VatRegisteredCompaniesRepository] =
    Future.fromTry(Try(new VatRegisteredCompaniesRepository(
      app.injector.instanceOf[MongoComponent], payloadBufferRepository, 1, 1.second,
      Configuration("microservice.services.vatRegisteredCompanies.unique-index.enabled" -> uniqueEnabled)
    ))).flatMap(repository => repository.initialised.map(_ => repository))

  override def beforeEach(): Unit = {
    dropMigrationIndexes()
    deleteAll()
    deleteAllBuffer()
    clearLock()
  }

  override def afterEach(): Unit = {
    try {
      dropMigrationIndexes()
      deleteAll()
      deleteAllBuffer()
      clearLock()
    } finally super.afterEach()
  }

  "Repository unique VAT-number index setup" should {
    "allow legacy duplicates while unique-index creation is disabled" in {
      insertOne(acmeTradingWithVatNo1)
      insertOne(deltaTradingWithVatNo1)

      val repository = initialiseRepository(uniqueEnabled = false).futureValue

      repository.collection.countDocuments().toFuture().futureValue shouldBe 2
      val indexes = repository.collection.listIndexes[BsonDocument]().toFuture().futureValue
      indexes.exists(index => index.getBoolean("unique", BsonBoolean(false)).getValue &&
        index.getDocument("key") == BsonDocument("vatNumber" -> 1)) shouldBe false
    }

    "fail initialization while legacy duplicates remain and unique-index creation is enabled" in {
      insertOne(acmeTradingWithVatNo1)
      insertOne(deltaTradingWithVatNo1)

      val failure = initialiseRepository(uniqueEnabled = true).failed.futureValue

      failure shouldBe a[MongoException]
      failure.asInstanceOf[MongoException].getCode shouldBe 11000
      totalCount shouldBe 2
      getRecord(testVatNo1) shouldBe Some(deltaTradingWithVatNo1)
    }

    "propagate a conflicting index definition instead of treating initialization as successful" in {
      await(collection.createIndex(Indexes.ascending("vatNumber"),
        IndexOptions().name(uniqueIndexName)).toFuture())

      val failure = initialiseRepository(uniqueEnabled = true).failed.futureValue

      failure shouldBe a[MongoException]
      Set(85, 86) should contain(failure.asInstanceOf[MongoException].getCode)
      val index = collection.listIndexes[BsonDocument]().toFuture().futureValue
        .find(_.getString("name").getValue == uniqueIndexName).get
      index.getBoolean("unique", BsonBoolean(false)).getValue shouldBe false
    }

    "initialize the same unique index successfully from two concurrent instances" in {
      val startupEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
      val start = new CyclicBarrier(2)

      try {
        val startups = (1 to 2).map { _ =>
          Future {
            start.await(10, TimeUnit.SECONDS)
            initialiseRepository(uniqueEnabled = true)
          }(using startupEc).flatMap(identity)
        }
        Future.sequence(startups).futureValue.size shouldBe 2

        val indexes = collection.listIndexes[BsonDocument]().toFuture().futureValue
        val uniqueIndexes = indexes.filter(_.getString("name").getValue == uniqueIndexName)
        uniqueIndexes.size shouldBe 1
        uniqueIndexes.head.getBoolean("unique", BsonBoolean(false)).getValue shouldBe true

        insertOne(acmeTradingWithVatNo1)
        val failure = collection.insertOne(Wrapper(testVatNo1, deltaTradingWithVatNo1))
          .toFuture().failed.futureValue
        failure shouldBe a[MongoException]
        failure.asInstanceOf[MongoException].getCode shouldBe 11000
        collection.find().toFuture().futureValue.toList shouldBe List(Wrapper(testVatNo1, acmeTradingWithVatNo1))
      } finally startupEc.shutdown()
    }

    "create and enforce the index on application startup while payload scheduling is disabled" in {
      stubAudit
      val enabledApp = new GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config ++ Map(
          "microservice.services.vatRegisteredCompanies.unique-index.enabled" -> "true",
          "metrics.name" -> "vat-number-unique-index-startup-it"
        ))
        .build()

      running(enabledApp) {
        val repository = enabledApp.injector.instanceOf[VatRegisteredCompaniesRepository]
        val buffer = enabledApp.injector.instanceOf[PayloadBufferRepository]
        val locks = enabledApp.injector.instanceOf[DefaultLockRepository]
        val service = enabledApp.injector.instanceOf[PersistenceService]
        repository.initialised.futureValue
        enabledApp.configuration.get[Boolean]("microservice.services.schedulers.payload.conversion.enabled") shouldBe false

        val indexes = repository.collection.listIndexes[BsonDocument]().toFuture().futureValue
        val index = indexes.find(_.getString("name").getValue == uniqueIndexName).get
        index.getDocument("key") shouldBe BsonDocument("vatNumber" -> 1)
        index.getBoolean("unique", BsonBoolean(false)).getValue shouldBe true

        val request = FakeRequest(POST, "/vat-registered-companies/vatregistrations")
          .withHeaders(AUTHORIZATION -> AUTHORISATION_TOKEN)
          .withJsonBody(Json.toJson(testPayloadCreateAndUpdates))
        status(route(enabledApp, request).get) shouldBe OK
        buffer.list.futureValue.map(_.payload) shouldBe List(testPayloadCreateAndUpdates)
        repository.collection.countDocuments().toFuture().futureValue shouldBe 0

        service.processOneData.futureValue
        repository.collection.find().toFuture().futureValue.toSet shouldBe Set(
          Wrapper(testVatNo1, acmeTradingWithVatNo1), Wrapper(testVatNo2, acmeTradingWithVatNo2)
        )
        repository.collection.countDocuments().toFuture().futureValue shouldBe 2
        buffer.list.futureValue shouldBe empty
        locks.isLocked(1).futureValue shouldBe false

        val failure = repository.collection.insertOne(Wrapper(testVatNo1, deltaTradingWithVatNo1))
          .toFuture().failed.futureValue
        failure shouldBe a[MongoException]
        failure.asInstanceOf[MongoException].getCode shouldBe 11000
      }
    }

    "preserve the latest update through reconciliation and enforce uniqueness afterwards" in {
      insertOne(acmeTradingWithVatNo1)
      insertOne(acmeTradingWithVatNo1.copy(name = "Intermediate company"))
      insertOne(deltaTradingWithVatNo1)
      getRecord(testVatNo1) shouldBe Some(deltaTradingWithVatNo1)

      val updated = deltaTradingWithVatNo1.copy(
        name = "Latest received company",
        address = deltaTradingWithVatNo1.address.copy(line1 = "Updated address")
      )
      insertOneBuffer(Payload(List(updated), Nil))
      persistenceService.processOneData.futureValue

      totalCount shouldBe 3
      getRecord(testVatNo1) shouldBe Some(updated)
      bufferTotalCount shouldBe 0
      isLocked shouldBe false

      persistenceService.deleteOld(1).futureValue
      totalCount shouldBe 2
      persistenceService.deleteOld(1).futureValue
      collection.find().toFuture().futureValue.toList shouldBe List(Wrapper(testVatNo1, updated))
      isLocked shouldBe false

      await(collection.createIndex(Indexes.ascending("vatNumber"),
        IndexOptions().name(nonUniqueIndexName)).toFuture())
      initialiseRepository(uniqueEnabled = true).futureValue
      initialiseRepository(uniqueEnabled = true).futureValue

      val failure = collection.insertOne(Wrapper(testVatNo1, acmeTradingWithVatNo1))
        .toFuture().failed.futureValue
      failure shouldBe a[MongoException]
      failure.asInstanceOf[MongoException].getCode shouldBe 11000

      val nextUpdate = updated.copy(name = "Updated after cutover")
      val payload = Payload(List(nextUpdate, acmeTradingWithVatNo2), Nil)
      (1 to 2).foreach { _ =>
        insertOneBuffer(payload)
        persistenceService.processOneData.futureValue
        collection.find().toFuture().futureValue.toSet shouldBe Set(
          Wrapper(testVatNo1, nextUpdate), Wrapper(testVatNo2, acmeTradingWithVatNo2)
        )
        totalCount shouldBe 2
        bufferTotalCount shouldBe 0
        isLocked shouldBe false
      }

      val restartedRepository = initialiseRepository(uniqueEnabled = false).futureValue
      val indexes = restartedRepository.collection.listIndexes[BsonDocument]().toFuture().futureValue
      val uniqueIndex = indexes.find(_.getString("name").getValue == uniqueIndexName).get
      uniqueIndex.getDocument("key") shouldBe BsonDocument("vatNumber" -> 1)
      uniqueIndex.getBoolean("unique", BsonBoolean(false)).getValue shouldBe true
      indexes.exists(_.getString("name").getValue == nonUniqueIndexName) shouldBe true
    }
  }
}
