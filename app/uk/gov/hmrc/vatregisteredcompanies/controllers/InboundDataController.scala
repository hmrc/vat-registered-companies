package uk.gov.hmrc.vatregisteredcompanies.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.mvc.Action
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class InboundDataController @Inject()(implicit executionContext: ExecutionContext) extends BaseController {

  def handle: Action[JsValue] = ???
}
