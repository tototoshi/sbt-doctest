package com.github.tkawachi.doctest

import StringUtil.{ escapeDoubleQuote => escapeDQ }

/**
 * Test generator for specs2.
 */
object Specs2TestGen extends TestGen {
  override def generate(basename: String, pkg: Option[String], examples: Seq[ParsedDoctest]): String = {
    val pkgLine = pkg.fold("")(p => s"package $p")
    s"""$pkgLine
       |
       |import org.scalacheck.Arbitrary._
       |import org.scalacheck.Prop._
       |
       |class ${basename}Doctest
       |    extends org.specs2.mutable.Specification
       |    with org.specs2.ScalaCheck {
       |
       |  def sbtDoctestTypeEquals[A](a1: => A)(a2: => A) = ()
       |
       |${examples.map(generateExample(basename, _)).mkString("\n\n")}
       |}
       |""".stripMargin
  }

  def generateExample(basename: String, parsed: ParsedDoctest): String = {
    s"""  "${escapeDQ(basename)}.scala:${parsed.lineNo}: ${parsed.symbol}" should {
       |${parsed.components.map(gen(parsed.lineNo, _)).mkString("\n\n")}
       |  }""".stripMargin
  }

  def gen(firstLine: Int, component: DoctestComponent): String =
    component match {
      case Example(expr, expected, _) =>
        val typeTest = expected.tpe.fold("")(tpe => genTypeTest(expr, tpe))
        s"""    "${componentDescription(component, firstLine)}" in {$typeTest
           |      ($expr).toString must_== "${escapeDQ(expected.value)}"
           |    }""".stripMargin
      case Property(prop, _) =>
        s"""    "${componentDescription(component, firstLine)}" ! prop {
           |      $prop
           |    }""".stripMargin
      case Verbatim(code) =>
        StringUtil.indent(code, "    ")
    }

  def genTypeTest(expr: String, expectedType: String): String = {
    s"""
       |      sbtDoctestTypeEquals($expr)(($expr): $expectedType)""".stripMargin
  }

  def componentDescription(comp: DoctestComponent, firstLine: Int): String = {
    def absLine(lineNo: Int): Int = firstLine + lineNo - 1
    def mkStub(s: String): String = escapeDQ(StringUtil.truncate(s))

    comp match {
      case Example(expr, _, lineNo) =>
        s"example at line ${absLine(lineNo)}: ${mkStub(expr)}"
      case Property(prop, lineNo) =>
        s"property at line ${absLine(lineNo)}: ${mkStub(prop)}"
      case _ => ""
    }
  }
}
