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

package uk.gov.hmrc.vatregisteredcompanies.filters

import play.api.Configuration
import javax.inject.{Inject, Singleton}
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SafeJsonErrorHandler @Inject()(
                                      auditConnector: AuditConnector,
                                      httpAuditEvent: HttpAuditEvent,
                                      configuration: Configuration
                                      )(implicit ec: ExecutionContext)
    extends JsonErrorHandler(auditConnector, httpAuditEvent, configuration){

    override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] =
      super.onServerError(withSafeUri(request), ex)

    private def withSafeUri(request: RequestHeader): RequestHeader = {
      val operation = request.attrs
        .get(Router.Attrs.HandlerDef)
        .fold("unrouted")(h => s"${h.controller}.${h.method}")

      request.withTarget(request.target.withUriString(s"operation=$operation"))
    }
}


