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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatregisteredcompanies.models.{Payload, PayloadSubmissionResponse => Response}
import uk.gov.hmrc.vatregisteredcompanies.services.{JsonSchemaChecker, PersistenceService}
import uk.gov.hmrc.vatregisteredcompanies.repositories.VatRegisteredCompaniesRepository
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.vatregisteredcompanies.models._

@Singleton
class InboundDataController @Inject()(persistence: PersistenceService, companiesRepo: VatRegisteredCompaniesRepository)
 (implicit executionContext: ExecutionContext, conf: Configuration, environment: Environment)
  extends BaseController with ExtraActions {

  def handle: Action[JsValue] =
    InboundDataAction.async(parse.json) { implicit request =>
      withJsonBody[Payload] { payload: Payload =>
        if (!JsonSchemaChecker[Payload](payload, "mdg-payload"))
          Future.successful(BadRequest(Json.toJson(
            Response(Response.Outcome.FAILURE, Response.Code.INVALID_PAYLOAD.some)
          )))
        else {
          companiesRepo.process(payload)
          Future.successful(Ok(Json.toJson(
            Response(Response.Outcome.SUCCESS, none)
          )))
        }
      }
    }

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = conf
}
