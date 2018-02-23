/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.xml

import java.io.FileNotFoundException

import org.mockito.ArgumentMatchers.{eq => ameq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.inventorylinking.imports.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec
import unit.util.TestData.xsdLocations
import unit.util.XMLTestData.{InvalidXML, InvalidXMLWithMultipleErrors, ValidInventoryLinkingMovementRequestXML}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Node, SAXException}

class XmlValidationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val MockConfiguration = mock[Configuration]
  private val MockXml = mock[Node]

  private def testService(test: XmlValidationService => Unit) = {
    test(new XmlValidationService(MockConfiguration))
  }

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }

  "XmlValidationService" should {
    "get location of xsd resource files from configuration" in testService { xmlValidationService =>
      await(xmlValidationService.validate(ValidInventoryLinkingMovementRequestXML))
      verify(MockConfiguration).getStringSeq(ameq("xsd.locations"))
    }

    "fail the future when in configuration there are no locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(None)

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "application.conf is missing mandatory property 'xsd.locations'"
    }

    "fail the future when in configuration there is an empty list for locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(Nil))

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "application.conf is missing mandatory property 'xsd.locations'"
    }

    "fail the future when a configured xsd resource file cannot be found" in testService { xmlValidationService =>
      when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(List("there/is/no/such/file")))

      val caught = intercept[FileNotFoundException] {
        await(xmlValidationService.validate(MockXml))
      }
      caught.getMessage shouldBe "XML Schema resource file: there/is/no/such/file"
    }

    "fail the future with SAXException when there is an error in XML" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidXML))
      }
      caught.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'InventoryLinkingImportsValidateMovementResponse'."
      Option(caught.getException) shouldBe None
    }

    "fail the future with wrapped SAXExceptions when there are multiple errors in XML" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidXMLWithMultipleErrors))
      }
      caught.getMessage shouldBe "cvc-type.3.1.3: The value 'A' of element 'entryVersionNumber' is not valid."

      Option(caught.getException) shouldBe 'nonEmpty

      val wrapped1 = caught.getException
      wrapped1.getMessage shouldBe "cvc-datatype-valid.1.2.1: 'A' is not a valid value for 'integer'."
      wrapped1.isInstanceOf[SAXException] shouldBe true

      Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
      val wrapped2 = wrapped1.asInstanceOf[SAXException].getException
      wrapped2.getMessage shouldBe "cvc-type.3.1.3: The value 'abc_123' of element 'entryNumber' is not valid."
      wrapped2.isInstanceOf[SAXException] shouldBe true

      Option(wrapped2.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
      val wrapped3 = wrapped2.asInstanceOf[SAXException].getException
      wrapped3.getMessage shouldBe "cvc-pattern-valid: Value 'abc_123' is not facet-valid with respect to pattern '([a-zA-Z0-9])*' for type 'entryNumber'."
      wrapped3.isInstanceOf[SAXException] shouldBe true

      Option(wrapped3.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
      val wrapped4 = wrapped3.asInstanceOf[SAXException].getException
      wrapped4.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'InventoryLinkingImportsValidateMovementResponse'."
      wrapped4.isInstanceOf[SAXException] shouldBe true

      Option(wrapped4.asInstanceOf[SAXException].getException) shouldBe None
    }

    "fail the future with configured number of wrapped SAXExceptions when there are multiple errors in XML" in testService {
      xmlValidationService =>
        when(MockConfiguration.getInt("xml.max-errors")).thenReturn(Some(2))

        val caught = intercept[SAXException] {
          await(xmlValidationService.validate(InvalidXMLWithMultipleErrors))
        }
        verify(MockConfiguration).getInt("xml.max-errors")

        caught.getMessage shouldBe "cvc-pattern-valid: Value 'abc_123' is not facet-valid with respect to pattern '([a-zA-Z0-9])*' for type 'entryNumber'."

        Option(caught.getException) shouldBe 'nonEmpty
        val wrapped1 = caught.getException
        wrapped1.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'InventoryLinkingImportsValidateMovementResponse'."
        wrapped1.isInstanceOf[SAXException] shouldBe true

        Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe None
    }

    "fail the future with system error when a configured maximum of xml errors is not a positive number" in testService {
      xmlValidationService =>
        when(MockConfiguration.getInt("xml.max-errors")).thenReturn(Some(0))

        val caught = intercept[IllegalArgumentException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "requirement failed: maxErrors should be a positive number but 0 was provided instead."
    }
  }
}
