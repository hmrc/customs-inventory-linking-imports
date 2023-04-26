/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.services

import java.io.FileNotFoundException

import org.mockito.Mockito.{verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.inventorylinking.imports.model.{GoodsArrival, ImportsMessageType, ValidateMovement}
import uk.gov.hmrc.customs.inventorylinking.imports.services.{GoodsArrivalXmlValidationService, ValidateMovementXmlValidationService, XmlValidationService}
import util.TestData._
import util.{TestData, UnitSpec}
import util.XMLTestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Node, SAXException}

class XmlValidationServiceSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp {
    protected val importsMessageType: ImportsMessageType
    protected lazy val mockConfiguration: Configuration = mock[Configuration]
    protected lazy val mockXml: Node = mock[Node]

    protected lazy val xsdPropertyPathLocation = s"xsd.locations.${importsMessageType.name}"

    protected def service: XmlValidationService = importsMessageType match {
      case _: GoodsArrival => new GoodsArrivalXmlValidationService(mockConfiguration)
      case _: ValidateMovement => new ValidateMovementXmlValidationService(mockConfiguration)
    }

    def elementName: String = TestData.elementName(importsMessageType)

    def otherElementName: String = TestData.otherElementName(importsMessageType)

    def xsdLocations: Seq[String] = importsMessageType match {
      case _: GoodsArrival => GoodsArrivalXsdLocations
      case _: ValidateMovement => ValidateMovementsXsdLocations
    }
  }

  private val testXmlDataByMessageType = Table(
    ("Message Type",
      "Valid xml",
      "Valid xml for other element",
      "Invalid xml with single error",
      "Invalid xml With multiple errors"),
    (new ValidateMovement(),
      ValidInventoryLinkingMovementRequestXML,
      ValidInventoryLinkingGoodsArrivalRequestXML,
      InvalidInventoryLinkingMovementRequestXML,
      InvalidInventoryLinkingMovementRequestXMLWithMultipleErrors),
    (new GoodsArrival(),
      ValidInventoryLinkingGoodsArrivalRequestXML,
      ValidInventoryLinkingMovementRequestXML,
      InvalidInventoryLinkingGoodsArrivalRequestXML,
      InvalidInventoryLinkingGoodsArrivalRequestXMLWithMultipleErrors)
  )

  forAll(testXmlDataByMessageType) { (messageType, validXml, _, _, _) =>
    s"$messageType XmlValidationService with valid input" should {
      s"not throw exceptions for valid XML for linking type ${messageType.name}" in new SetUp {
        protected val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        await(service.validate(validXml))

        verify(mockConfiguration).getOptional[Seq[String]](xsdPropertyPathLocation)
      }
    }
  }

  forAll(testXmlDataByMessageType) { (messageType, _, validXmlForOtherElement, invalidXml, invalidXmlWithMultipleErrors) =>
    s"$messageType XmlValidationService with invalid input" should {
      "fail the future when in configuration there are no locations of xsd resource files" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(None)
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[IllegalStateException] {
          await(service.validate(mockXml))
        }
        caught.getMessage shouldBe s"application.conf is missing mandatory property '$xsdPropertyPathLocation'"
      }

      s"fail the future when in configuration there is an empty list for locations of xsd resource files" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(Nil))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[IllegalStateException] {
          await(service.validate(mockXml))
        }
        caught.getMessage shouldBe s"application.conf is missing mandatory property '$xsdPropertyPathLocation'"
      }

      "fail the future when a configured xsd resource file cannot be found" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(List("there/is/no/such/file")))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[FileNotFoundException] {
          await(service.validate(mockXml))
        }
        caught.getMessage shouldBe "XML Schema resource file: there/is/no/such/file"
      }

      "fail the future with SAXException when there is an valid XML for other XSD" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[SAXException] {
          await(service.validate(validXmlForOtherElement))
        }


        caught.getMessage shouldBe s"cvc-elt.1.a: Cannot find the declaration of element '$otherElementName'."
        Option(caught.getException) shouldBe None
      }

      "fail the future with SAXException when there is an error in XML" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[SAXException] {
          await(service.validate(invalidXml))
        }

        caught.getMessage shouldBe s"cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element '$elementName'."
        Option(caught.getException) shouldBe None
      }

      "fail the future with wrapped SAXExceptions when there are multiple errors in XML" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)

        private val caught = intercept[SAXException] {
          await(service.validate(invalidXmlWithMultipleErrors))
        }

        caught.getMessage shouldBe "cvc-type.3.1.3: The value 'A' of element 'entryVersionNumber' is not valid."
        Option(caught.getException) shouldBe 'nonEmpty

        private val wrapped1 = caught.getException
        wrapped1.getMessage shouldBe "cvc-datatype-valid.1.2.1: 'A' is not a valid value for 'integer'."
        wrapped1.isInstanceOf[SAXException] shouldBe true

        Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
        private val wrapped2 = wrapped1.asInstanceOf[SAXException].getException
        wrapped2.getMessage shouldBe "cvc-type.3.1.3: The value 'abc_123' of element 'entryNumber' is not valid."
        wrapped2.isInstanceOf[SAXException] shouldBe true

        Option(wrapped2.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
        private val wrapped3 = wrapped2.asInstanceOf[SAXException].getException
        wrapped3.getMessage shouldBe "cvc-pattern-valid: Value 'abc_123' is not facet-valid with respect to pattern '([a-zA-Z0-9])*' for type 'entryNumber'."
        wrapped3.isInstanceOf[SAXException] shouldBe true

        Option(wrapped3.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
        private val wrapped4 = wrapped3.asInstanceOf[SAXException].getException
        wrapped4.getMessage shouldBe s"cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element '$elementName'."
        wrapped4.isInstanceOf[SAXException] shouldBe true

        Option(wrapped4.asInstanceOf[SAXException].getException) shouldBe None
      }

      "fail the future with configured number of wrapped SAXExceptions when there are multiple errors in XML" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(Some(2))

        private val caught = intercept[SAXException] {
          await(service.validate(invalidXmlWithMultipleErrors))
        }
        verify(mockConfiguration).getOptional[Int]("xml.max-errors")

        caught.getMessage shouldBe "cvc-pattern-valid: Value 'abc_123' is not facet-valid with respect to pattern '([a-zA-Z0-9])*' for type 'entryNumber'."

        Option(caught.getException) shouldBe 'nonEmpty
        private val wrapped1 = caught.getException
        wrapped1.getMessage shouldBe s"cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element '$elementName'."
        wrapped1.isInstanceOf[SAXException] shouldBe true

        Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe None
      }

      "fail the future with system error when a configured maximum of xml errors is not a positive number" in new SetUp {
        val importsMessageType: ImportsMessageType = messageType
        when(mockConfiguration.getOptional[Seq[String]](xsdPropertyPathLocation)).thenReturn(Some(xsdLocations))
        when(mockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(Some(0))

        private val caught = intercept[IllegalArgumentException] {
          await(service.validate(mockXml))
        }
        caught.getMessage shouldBe "requirement failed: maxErrors should be a positive number but 0 was provided instead."
      }
    }
  }
}
