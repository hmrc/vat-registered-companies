/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vatregisteredcompanies.models.{AuditDetails, ConsultationNumber, LookupResponse, VatNumber}
import uk.gov.hmrc.vatregisteredcompanies.services.PersistenceService

import scala.concurrent.ExecutionContext

class VatRegCoLookupController @Inject()
(
  persistence: PersistenceService,
  auditConnector: AuditConnector,
  cc: ControllerComponents
)(
  implicit executionContext: ExecutionContext
) extends BackendController(cc) {

  def lookup(target: VatNumber): Action[AnyContent] =
    Action.async {
      persistence.lookup(target).map { x =>
        Ok(Json.toJson(x.getOrElse(LookupResponse(None))))
      }
    }

  def lookupVerified(target: VatNumber, requester: VatNumber): Action[AnyContent] = {
    Action.async { implicit request =>
      val targetLookup = persistence.lookup(target)
      val requesterLookup = persistence.lookup(requester)

      val lr = for {
        a <- targetLookup
        b <- requesterLookup
      } yield (a,b) match {
        case (Some(t), None) => t
        case (Some(t), Some(r)) =>
          t.copy(requester = r.target.map(x => x.vatNumber), consultationNumber = Some(ConsultationNumber.generate))
        case (_, Some(r)) => LookupResponse(None, r.target.map(x => x.vatNumber))
        case _ => LookupResponse(None)
      }
      lr.map {x =>
        auditVerifiedLookup(x.some)
        Ok(Json.toJson(x))
      }
    }
  }

  private def auditVerifiedLookup(result: Option[LookupResponse])(implicit headerCarrier: HeaderCarrier): Unit = {
    result match {
      case Some(LookupResponse(Some(targetCompany),Some(requesterVatNumber),Some(consultationNumber),processingDate)) =>
        val auditDetails = AuditDetails(
          requesterVatNumber,
          targetCompany.vatNumber,
          targetCompany,
          consultationNumber,
          processingDate)
        auditConnector.sendExplicitAudit(
          "verifiedVATRegisteredCompanyCheck",
          Json.toJson(auditDetails)
        )
      case _ => ()
    }
  }

}