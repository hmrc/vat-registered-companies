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

package uk.gov.hmrc.vatregisteredcompanies.testsupport

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger as LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters.*

trait LogCapture {
  
  def capturedLogs(body: => Unit): List[String] = {
    val root             = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
    val previousLevel    = root.getLevel
    val appender         = new ListAppender[ILoggingEvent]
    
    appender.start()
    root.setLevel(Level.DEBUG)
    root.addAppender(appender)
    
    try body
    finally {
      root.detachAppender(appender)
      root.setLevel(previousLevel)
      appender.stop()
    }
    
    appender.list.asScala.toList.map { event => 
      val thrown = Option(event.getThrowableProxy)
        .map(t => s" ${t.getClassName}: ${t.getMessage}")
        .getOrElse("")
      s"LoggerName: ${event.getLoggerName} MessageName: ${event.getFormattedMessage}$thrown"
    }
  }

}
