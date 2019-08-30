/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.vatregisteredcompanies.services

import cats.data.OptionT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber}
import uk.gov.hmrc.vatregisteredcompanies.repositories.{LockRepository, PayloadBufferRepository, PayloadWrapper, VatRegisteredCompaniesRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersistenceService @Inject()(
  repository: VatRegisteredCompaniesRepository,
  buffer: PayloadBufferRepository,
  lockRepository: LockRepository
)(implicit executionContext: ExecutionContext) {

  def deleteAll: Future[Unit] = {
    val bd = buffer.deleteAll
    val rd = repository.deleteAll
    for {
      _ <- bd
      _ <- rd
    } yield {
      Logger.info("Finished delete all")
    }
  }

  def lookup(target: VatNumber): Future[Option[LookupResponse]] =
    repository.lookup(target)

  def deleteOld(n: Int): Future[Unit] =
    withLock(1)(repository.deleteOld(n))

  def processOneData: Future[Unit] = {
    val x = for {
      bp <- OptionT(buffer.one)
      _  <- OptionT.liftF(withLock(1)(repository.process(bp)))
    } yield {}

    x.fold((())) {_=> (())}
  }

  def bufferData(payload: Payload): Future[Unit] =
    buffer.insert(payload)

  def retrieveBufferData: Future[List[PayloadWrapper]] =
    buffer.list

  def reportIndexes: Future[Unit] = {
    for {
      list <- repository.collection.indexesManager.list()
    } yield list.foreach(index => Logger.warn(s"Found mongo index ${index.name}"))
  }

  private def withLock(id: Int)(f: => Future[Unit]): Future[Unit] = {
    lockRepository.lock(id).flatMap {
      gotLock =>
        if (gotLock) {
          f.flatMap {
            result =>
              lockRepository.release(id).map {
                _ => {
                  result
                }
              }
          }.recoverWith {
            case e =>
              lockRepository.release(id)
                .map { _ => throw e }
          }
        } else {
          Future.successful(())
        }
    }
  }

}


