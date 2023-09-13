package indigoplugin.generators

import indigoplugin.IndigoAssets

object AssetListing {

  def generate(
      outDir: os.Path,
      moduleName: String,
      fullyQualifiedPackage: String,
      indigoAssets: IndigoAssets
  ): Seq[os.Path] = {

    val fileContents: String =
      renderContent(indigoAssets.listAssetFiles)

    val wd = outDir / Generators.OutputDirName

    os.makeDir.all(wd)

    val file = wd / s"$moduleName.scala"

    val contents =
      s"""package $fullyQualifiedPackage
      |
      |import indigo.*
      |
      |object $moduleName:
      |
      |${fileContents}
      |
      |""".stripMargin

    os.write.over(file, contents)

    Seq(file)
  }

  def renderContent(paths: List[os.RelPath]): String =
    (convertPathsToTree _ andThen renderTree(0))(paths)

  def convertPathsToTree(paths: List[os.RelPath]): PathTree =
    PathTree
      .combineAll(
        paths.map { p =>
          PathTree.pathToPathTree(p) match {
            case None        => throw new Exception(s"Could not parse given path: $p")
            case Some(value) => value
          }
        }
      )
      .sorted

  def renderTree(indent: Int)(pathTree: PathTree): String =
    pathTree match {
      case PathTree.File(_, _, _) =>
        ""

      case PathTree.Folder(folderName, children) =>
        renderFolderContents(folderName, children, indent)

      case PathTree.Root(children) =>
        renderFolderContents("", children, indent)
    }

  def renderFolderContents(folderName: String, children: List[PathTree], indent: Int): String = {
    val indentSpaces               = List.fill(indent)("  ").mkString
    val indentSpacesNext           = indentSpaces + "  "
    val safeFolderName             = toSafeName(folderName)
    val files: List[PathTree.File] = children.collect { case f: PathTree.File => f }

    val renderedFiles: List[(String, String)] =
      files
        .map {
          case PathTree.File(name, ext, path) if AudioFileExtensions.contains(ext) =>
            val safeName = toSafeName(name)

            val vals =
              s"""${indentSpacesNext}val ${safeName}: AssetName            = AssetName("${name}.${ext}")
              |${indentSpacesNext}val ${safeName}Play: PlaySound        = PlaySound(${safeName}, Volume.Max)
              |${indentSpacesNext}val ${safeName}SceneAudio: SceneAudio = SceneAudio(SceneAudioSource(BindingKey("${name}.${ext}"), PlaybackPattern.SingleTrackLoop(Track(${safeName}))))""".stripMargin

            val loadable =
              s"""${indentSpacesNext}    AssetType.Audio(${safeName}, AssetPath(baseUrl + "${path}"))"""

            (vals, loadable)

          case PathTree.File(name, ext, path) if ImageFileExtensions.contains(ext) =>
            val safeName = toSafeName(name)

            val vals =
              s"""${indentSpacesNext}val ${safeName}: AssetName               = AssetName("${name}.${ext}")
              |${indentSpacesNext}val ${safeName}Material: Material.Bitmap = Material.Bitmap(${safeName})""".stripMargin

            val tag =
              if (safeFolderName.isEmpty) "None"
              else s"""Option(AssetTag("${safeFolderName}"))"""

            val loadable =
              s"""${indentSpacesNext}    AssetType.Image(${safeName}, AssetPath(baseUrl + "${path}"), ${tag})"""

            (vals, loadable)

          case PathTree.File(name, ext, path) =>
            val safeName = toSafeName(name)

            val vals =
              s"""${indentSpacesNext}val ${safeName}: AssetName = AssetName("${name}.${ext}")"""

            val loadable =
              s"""${indentSpacesNext}    AssetType.Text(${safeName}, AssetPath(baseUrl + "${path}"))"""

            (vals, loadable)
        }

    val assetSeq: String = {
      if (files.isEmpty) ""
      else
        s"""${renderedFiles.map(_._1).mkString("\n")}
        |
        |${indentSpacesNext}def assets(baseUrl: String): Set[AssetType] =
        |${indentSpacesNext}  Set(
        |${renderedFiles.map(_._2).mkString(",\n")}
        |${indentSpacesNext}  )
        |
        |""".stripMargin
    }

    val contents =
      s"""${children.map(renderTree(indent + 1)).mkString}""".stripMargin + assetSeq

    if (safeFolderName.isEmpty) contents
    else
      s"""${indentSpaces}object ${safeFolderName}:
      |${contents}"""
  }

  def toSafeName(name: String): String =
    name.replaceAll("[^a-zA-Z0-9]", "-").split("-").toList.filterNot(_.isEmpty) match {
      case h :: t if h.take(1).matches("[0-9]") => ("_" :: h :: t.map(_.capitalize)).mkString
      case h :: t                               => (h :: t.map(_.capitalize)).mkString
      case l                                    => l.map(_.capitalize).mkString
    }

  val AudioFileExtensions: Set[String] =
    Set(
      "aac",
      "cda",
      "mid",
      "midi",
      "mp3",
      "oga",
      "ogg",
      "opus",
      "wav",
      "weba",
      "flac"
    )

  val ImageFileExtensions: Set[String] =
    Set(
      "apng",
      "avif",
      "gif",
      "jpg",
      "jpeg",
      "jfif",
      "pjpeg",
      "pjp",
      "png",
      "svg",
      "webp",
      "bmp",
      "ico",
      "cur",
      "tif",
      "tiff"
    )

}
