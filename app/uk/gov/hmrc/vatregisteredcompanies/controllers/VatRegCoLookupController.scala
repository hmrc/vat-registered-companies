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

package uk.gov.hmrc.vatregisteredcompanies.controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatregisteredcompanies.models.{ConsultationNumber, LookupResponse, VatNumber}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.{ExecutionContext, Future}

class VatRegCoLookupController @Inject()(persistence: PersistenceService, auditConnector: AuditConnector)(implicit executionContext: ExecutionContext) extends BaseController {

  def lookup(target: VatNumber): Action[AnyContent] =
    Action.async { implicit request =>
      persistence.lookup(target).map { x =>
        Ok(Json.toJson(x.getOrElse(LookupResponse(None))))
      }
    }

  def lookupVerified(target: VatNumber, requester: VatNumber): Action[AnyContent] = {
    Action.async { implicit request =>
      val targetLookup = persistence.lookup(target)
      val requesterLookup = persistence.lookup(requester)
      val futureResult: Future[Option[LookupResponse]] = for {
        a <- targetLookup
        b <- requesterLookup
      } yield a.map(x => x.copy(
        requester = b.fold(Option.empty[VatNumber])(_ => Some(requester)),
        consultationNumber = b.fold(Option.empty[ConsultationNumber])(_ => Some(ConsultationNumber.generate))
      ))

      futureResult.map {x =>
        auditVerifiedLookup(x)
        Ok(Json.toJson(x.getOrElse(LookupResponse(None))))
      }
    }
  }

  private def auditVerifiedLookup(result: Option[LookupResponse])(implicit headerCarrier: HeaderCarrier): Unit = {
    result match {
      case Some(LookupResponse(Some(a),Some(b),Some(c),d)) =>
        val details = Map[String, String](
          "check on VAT number: " -> a.vatNumber,
          "check by company with VAT number: " -> b,
          "generated consultation number: " -> c,
          "processing date" -> d.toString
        )
        auditConnector.sendExplicitAudit(
          "Verified VAT registered company check",
          details
        )
      case _ => ()
    }
  }

}