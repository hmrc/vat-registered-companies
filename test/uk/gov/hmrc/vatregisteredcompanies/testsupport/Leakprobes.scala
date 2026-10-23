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

object Leakprobes {
  
  final case class  Probe(label: String, value: String, forbidden: Seq[String])
  
  val all: Seq[Probe] = Seq(
    Probe(
      label = "an email address",
      value = "leakcheck-7f3a@example.com",
      forbidden = Seq("leakcheck-7f3a", "example.com", "%40")
    ),

    Probe(
      label = "a VAT-number-shaped value",
      value = "123456789012", 
      forbidden = Seq("123456789012")
    ),

    Probe(
      label = "a company-name-shaped value",
      value = "leakcheck-acme-widgets-Ltd",
      forbidden = Seq("leakcheck-acme-widgets-Ltd")
    ),

    Probe(
      label = "a value with a query string",
      value = "leakcheck-1a2b?name=leakcheck-3c-4d&token=leakcheck-5e6f",
      forbidden = Seq("leakcheck-1a2b", "leakcheck-3c4d", "leakcheck-5e6f")
    )
  )  
  
    def leakingLines(logs: List[String], probe: Probe): List[String] =
      logs.filter(line => probe.forbidden.exists(forbiddenText => line.contains(forbiddenText)))

}
