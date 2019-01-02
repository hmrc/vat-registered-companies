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

import java.time.LocalDateTime

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Action
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatregisteredcompanies.models.{Payload, PayloadSubmissionResponse => Response}
import uk.gov.hmrc.vatregisteredcompanies.services.{JsonSchemaChecker, PersistenceService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InboundDataController @Inject()(persistence: PersistenceService)(implicit executionContext: ExecutionContext) extends BaseController {

  def handle: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      withJsonBody[Payload] {
        case payload if !JsonSchemaChecker[Payload](payload, "mdg-payload") =>
          Future(Ok(Json.toJson(Response(Response.failure, LocalDateTime.now, Response.invalidPayload.some))))
        case payload =>
          persistence.processData(payload).map { _ =>
            Ok(Json.toJson(Response(Response.success, LocalDateTime.now, none)))
          }
      }
    } // TODO send other error code

}
