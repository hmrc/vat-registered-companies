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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.vatregisteredcompanies.models.{LookupResponse, Payload, VatNumber}
import uk.gov.hmrc.vatregisteredcompanies.repositories.{PayloadBufferRepository, PayloadWrapper, VatRegisteredCompaniesRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersistenceService @Inject()(repository: VatRegisteredCompaniesRepository, buffer: PayloadBufferRepository)(implicit executionContext: ExecutionContext) {

  def lookup(target: VatNumber): Future[Option[LookupResponse]] =
    repository.lookup(target)

  def processData: Future[Unit] =  {
    for {
      bd <- retrieveBufferData
      _  <- repository.processList(bd)
      _  <- cleanBuffer(bd)
    } yield {}
  }

  def processOneData: Future[Unit] = {
    for {
      bp <- buffer.one
      _  <- repository.process(bp.payload)
      _  <- buffer.deleteOne(bp)
    } yield {}
  }

  def bufferData(payload: Payload): Future[Unit] =
    buffer.insert(payload)

  def retrieveBufferData: Future[List[PayloadWrapper]] =
    buffer.list

  def cleanBuffer(payloadWrapperList: List[PayloadWrapper]): Future[Unit] =
    buffer.deleteMany(payloadWrapperList)

}


