package indigoplugin

import indigoplugin.templates.ElectronTemplates

import scala.sys.process._
import os._
import indigoplugin.templates.SupportScriptTemplate

object IndigoRun {

  private val installingNPMDepsMessage: String =
    " > Installing NPM dependencies"

  private val usingInstalledNPMDepsMessage: String =
    " > Using already installed NPM dependencies"

  def run(
      outputDir: Path,
      buildDir: Path,
      title: String,
      windowWidth: Int,
      windowHeight: Int,
      disableFrameRateLimit: Boolean,
      electronInstall: ElectronInstall
  ): Unit = {

    os.makeDir.all(outputDir)

    filesToWrite(windowWidth, windowHeight, disableFrameRateLimit, electronInstall).foreach { f =>
      os.write.over(outputDir / f.name, f.contents)
    }

    os.list(buildDir).foreach { file =>
      os.copy(file, outputDir / file.last, true, true, true, true, true)
    }

    // Write support js script
    val supportFile = outputDir / "scripts" / "indigo-support.js"
    val support     = SupportScriptTemplate.template()
    os.remove(supportFile)
    os.write(supportFile, support)

    println(s"Starting '$title'")

    sys.props("os.name").toLowerCase match {
      case x if x contains "windows" =>
        electronInstall match {
          case ElectronInstall.Global =>
            IndigoProc.Windows.npmStart(outputDir)

          case ElectronInstall.Version(_) =>
            if (!os.exists(outputDir / "node_modules" / "electron")) {
              println(installingNPMDepsMessage)
              IndigoProc.Windows.npmInstall(outputDir)
            } else {
              println(usingInstalledNPMDepsMessage)
            }
            IndigoProc.Windows.npmStart(outputDir)

          case ElectronInstall.Latest =>
            if (!os.exists(outputDir / "node_modules" / "electron")) {
              println(installingNPMDepsMessage)
              IndigoProc.Windows.installLatestElectron(outputDir)
              IndigoProc.Windows.npmInstall(outputDir)
            } else {
              println(usingInstalledNPMDepsMessage)
            }
            IndigoProc.Windows.npmStart(outputDir)

          case ElectronInstall.PathToExecutable(path) =>
            IndigoProc.Windows.npmStart(outputDir)
        }

      case _ =>
        electronInstall match {
          case ElectronInstall.Global =>
            IndigoProc.Nix.npmStart(outputDir)

          case ElectronInstall.Version(_) =>
            if (!os.exists(outputDir / "node_modules" / "electron")) {
              println(installingNPMDepsMessage)
              IndigoProc.Nix.npmInstall(outputDir)
            } else {
              println(usingInstalledNPMDepsMessage)
            }
            IndigoProc.Nix.npmStart(outputDir)

          case ElectronInstall.Latest =>
            if (!os.exists(outputDir / "node_modules" / "electron")) {
              println(installingNPMDepsMessage)
              IndigoProc.Nix.installLatestElectron(outputDir)
              IndigoProc.Nix.npmInstall(outputDir)
            } else {
              println(usingInstalledNPMDepsMessage)
            }
            IndigoProc.Nix.npmStart(outputDir)

          case ElectronInstall.PathToExecutable(path) =>
            IndigoProc.Nix.npmStart(outputDir)
        }
    }

    ()
  }

  def filesToWrite(
      windowWidth: Int,
      windowHeight: Int,
      disableFrameRateLimit: Boolean,
      electronInstall: ElectronInstall
  ): List[FileToWrite] =
    List(
      FileToWrite("main.js", ElectronTemplates.mainFileTemplate(windowWidth, windowHeight)),
      FileToWrite("preload.js", ElectronTemplates.preloadFileTemplate),
      FileToWrite("package.json", ElectronTemplates.packageFileTemplate(disableFrameRateLimit, electronInstall))
    )

}

object IndigoProc {

  object Windows {
    def npmStart(outputDir: Path) =
      os.proc("cmd", "/C", "npm", "start")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)

    def installLatestElectron(outputDir: Path) =
      os.proc("cmd", "/C", "npm", "install", "electron", "--save-dev")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)

    def npmInstall(outputDir: Path) =
      os.proc("cmd", "/C", "npm", "install")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
  }

  object Nix {
    def npmStart(outputDir: Path) =
      os.proc("npm", "start")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)

    def installLatestElectron(outputDir: Path) =
      os.proc("npm", "install", "electron", "--save-dev")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)

    def npmInstall(outputDir: Path) =
      os.proc("npm", "install")
        .call(cwd = outputDir, stdin = os.Inherit, stdout = os.Inherit, stderr = os.Inherit)
  }

}
