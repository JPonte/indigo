package indigoplugin.generators

import indigoplugin.IndigoGenerators

class GeneratorAcceptanceTests extends munit.FunSuite {

  val sourceCSV = os.pwd / "test-assets" / "data" / "stats.csv"
  val sourceMD  = os.pwd / "test-assets" / "data" / "stats.md"

  val targetDir = os.pwd / "out" / "indigo-plugin-generator-acceptance-test-output"

  override def beforeAll(): Unit = {
    if (os.exists(targetDir)) {
      os.remove.all(target = targetDir)
    }

    os.makeDir.all(targetDir)
  }

  test("Can generate an enum from a CSV file") {

    val files =
      IndigoGenerators
        .default(targetDir, "com.example.test")
        .embedCSV
        .asEnum("StatsEnum", sourceCSV)
        .toSources

    files.headOption match {
      case None =>
        fail("No file was generated")

      case Some(src) =>
        assert(clue(src) == clue(targetDir / "indigo-compile-codegen-output" / "StatsEnum.scala"))

        val actual = os.read(src)

        val expected =
          """
          |package com.example.test
          |
          |// DO NOT EDIT: Generated by Indigo.
          |
          |enum StatsEnum(val level: Int, val bonus: Int):
          |  case Intelligence extends StatsEnum(2, 4)
          |  case Strength extends StatsEnum(10, 0)
          |  case Fortitude extends StatsEnum(4, 1)
          |""".stripMargin

        assertEquals(actual.trim, expected.trim)
    }
  }

  test("Can generate a map from a markdown table file") {

    val files =
      IndigoGenerators
        .default(targetDir, "com.example.test")
        .embedMarkdownTable
        .asMap("StatsMap", sourceMD)
        .toSources

    files.headOption match {
      case None =>
        fail("No file was generated")

      case Some(src) =>
        assert(clue(src) == clue(targetDir / "indigo-compile-codegen-output" / "StatsMap.scala"))

        val actual = os.read(src)

        val expected =
          """
          |package com.example.test
          |
          |// DO NOT EDIT: Generated by Indigo.
          |
          |final case class StatsMap(val level: Int, val bonus: Int, val code: String)
          |object StatsMap:
          |  val data: Map[String, StatsMap] =
          |    Map(
          |      "intelligence" -> StatsMap(2, 4, "i"),
          |      "strength" -> StatsMap(10, 0, "st"),
          |      "fortitude" -> StatsMap(4, 1, "_frt")
          |    )
          |""".stripMargin

        assertEquals(actual.trim, expected.trim)
    }
  }

  test("Can generate a map from a markdown table file (armour, unformatted)") {

    val files =
      IndigoGenerators
        .default(targetDir, "com.example.test")
        .embedMarkdownTable
        .asEnum("Armour", os.pwd / "test-assets" / "data" / "armour.md")
        .toSources

    files.headOption match {
      case None =>
        fail("No file was generated")

      case Some(src) =>
        assert(clue(src) == clue(targetDir / "indigo-compile-codegen-output" / "Armour.scala"))

        val actual = os.read(src)

        val expected =
          """
          |package com.example.test
          |
          |// DO NOT EDIT: Generated by Indigo.
          |
          |enum Armour(val defenseBonus: Int):
          |  case LeatherArmor extends Armour(1)
          |  case ChainMail extends Armour(3)
          |""".stripMargin

        assertEquals(actual.trim, expected.trim)
    }
  }

  test("Can generate a custom output from a markdown table file") {

    val files =
      IndigoGenerators
        .default(targetDir, "com.example.test")
        .embedMarkdownTable
        .asCustom("StatsMap", sourceMD) { data =>
          s"""/*
          |${data.map(_.map(cell => s"${cell.asString}: ${cell.giveTypeName}").mkString(",")).mkString("\n")}
          |*/""".stripMargin.trim
        }
        .toSources

    files.headOption match {
      case None =>
        fail("No file was generated")

      case Some(src) =>
        assert(clue(src) == clue(targetDir / "indigo-compile-codegen-output" / "StatsMap.scala"))

        val actual = os.read(src)

        val expected =
          """
          |package com.example.test
          |
          |// DO NOT EDIT: Generated by Indigo.
          |/*
          |"name": String,"level": String,"bonus": String,"code": String
          |"intelligence": String,2: Int,4: Int,"i": String
          |"strength": String,10: Int,0: Int,"st": String
          |"fortitude": String,4: Int,1: Int,"_frt": String
          |*/
          |""".stripMargin

        assertEquals(actual.trim, expected.trim)
    }
  }

  test("Can generate Aseprite Data") {

    val jsonFile = os.pwd / "test-assets" / "captain" / "Captain Clown Nose Data.json"

    val files =
      IndigoGenerators
        .default(targetDir, "com.example.test")
        .embedAseprite("MyAnimation", jsonFile)
        .toSources

    files.headOption match {
      case None =>
        fail("No file was generated")

      case Some(src) =>
        assert(clue(src) == clue(targetDir / "indigo-compile-codegen-output" / "MyAnimation.scala"))

        val actual = os.read(src)

        val expected =
          """
          |package com.example.test
          |
          |import indigo.shared.formats.*
          |
          |// DO NOT EDIT: Generated by Indigo.
          |object MyAnimation:
          |
          |  val aseprite: Aseprite =
          |    Aseprite(List(AsepriteFrame("Captain Clown Nose 0.aseprite"
          |""".stripMargin

        assert(clue(actual.trim).startsWith(clue(expected.trim)))
    }

  }

}
