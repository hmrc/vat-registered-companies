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

import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

trait ExtraActions {

  lazy val logger: Logger = Logger(this.getClass)

  def servicesConfig: ServicesConfig
  def messagesControllerComponents: MessagesControllerComponents
  val InboundDataAction: ActionBuilder[Request, AnyContent] = AuthorisedFilterAction
  val bearerToken = s"Bearer ${servicesConfig.getConfString("mdg.inboundData.token", "")}"

  object AuthorisedFilterAction extends ActionBuilder[Request, AnyContent] with ActionFilter[Request] {
    override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
      Future.successful(
        request.headers.get(HeaderNames.AUTHORIZATION).fold[Option[Result]] {
          Some(Unauthorized(s"No ${HeaderNames.AUTHORIZATION} present"))
        } {
          a =>
            if (a.matches(bearerToken))
              None
            else {
              logger.info(s"config token ends with ${bearerToken.toList.takeRight(3).mkString}")
              logger.info(s"config token is ${bearerToken.length} characters long")
              logger.info(s"config token starts with 'Bearer ' ${bearerToken.startsWith("Bearer ")}")
              logger.info(s"supplied token starts with 'Bearer ' ${a.startsWith("Bearer ")}")
              logger.info(s"supplied token ends with ${a.toList.takeRight(3).mkString}")
              logger.info(s"supplied token is ${a.length} characters long")

              Some(Unauthorized("Supplied bearer token does not match config"))
            }
        }
      )
    }

    override def parser: BodyParser[AnyContent] = messagesControllerComponents.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = messagesControllerComponents.executionContext
  }

}
