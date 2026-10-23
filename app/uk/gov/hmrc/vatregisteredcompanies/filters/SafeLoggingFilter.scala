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

import javax.inject.Inject
import org.apache.pekko.stream.Materializer
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router.Attrs
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ControllerConfigs
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SafeLoggingFilter @Inject()(config: ControllerConfigs)(
                                                              implicit
                                                              override val mat: Materializer,
                                                              ec: ExecutionContext
                                                            ) extends Filter {
  
  private val logger = Logger(getClass)
  
  override def apply(next: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val start = System.currentTimeMillis()
    val handlerDef = rh.attrs.get(Attrs.HandlerDef)
    
    if(!handlerDef.forall(h => config.get(h.controller).logging)){
      next(rh)
    }
    else {
      val operation = handlerDef.fold("unrouted")(h => s"${h.controller}.${h.method}")
      val requestId = HeaderCarrierConverter.fromRequest(rh).requestId.fold("-")(_.value)
      def elapsed   = System.currentTimeMillis() - start
      
      next(rh).andThen{
        case scala.util.Success(result) => 
          logger.info(s"operation=$operation requestId=$requestId status=${result.header.status} durationMs=$elapsed")
        case scala.util.Failure(NonFatal(t)) => 
          logger.info(s"operation=$operation requestId=$requestId error=${t.getClass.getSimpleName} durationMs=$elapsed")
      }
      
    }
  }

}
