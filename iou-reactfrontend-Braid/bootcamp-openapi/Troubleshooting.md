Here is a list of common issues encountered during the bootcamp, and how to fix them.

* `java.io.NotSerializableException: net.corda.core.contracts.TransactionState -> data(net.corda.core.contracts.ContractState) -> Constructor parameter - "arg0" -  doesn't refer to a property of "class java_bootcamp.TokenState" -> class java_bootcamp.TokenState`

  * **Cause**: The Corda serialisation framework requires the Java compiler to preserve the argument names when generating bytecode, so that objects can be correctly recreated later

  * **Fix**: In IntelliJ:
    * Open the settings:
      * Windows: `File -> Settings`
      * osX: `IntelliJ IDEA -> Preferences...`
    * Go to `Build, Execution, Deployment -> Compiler -> Java Compiler`
    * Write `-parameters` in the `Additional command line parameters` field
    * Completely rebuild the project (`Build` -> `Rebuild Project`)

* `java.lang.IllegalStateException: Missing the '-javaagent' JVM argument.` when running flow tests

  * **Cause**: The Quasar library must be used to instrument the flows so that the flows can be checkpointed correctly

  * **Fix**: Open the run configuration used to run the test and add the `-javaagent:lib/quasar.jar` flag to `VM options:` (see [here](https://www.jetbrains.com/help/idea/creating-and-editing-run-debug-configurations.html))

* `Invalid Gradle JDK configuration found... Project JDK is not specified.` when importing the project using Gradle

  * **Cause**: You need to tell IntelliJ which JDK to use for your project

  * **Fix**: Click `File > Project Structure…` and select the `Project SDK`. Add a new SDK if required by clicking `New…` and selecting the folder containing the JDK. You'll need to close and re-open the project before attempting to import the Gradle project again

* `net.corda.core.transactions.MissingContractAttachments: Cannot find contract attachments for [java_bootcamp.TokenContract].` when running contract tests

  * **Cause**: Your `TokenContract` does not implement the `Contract` interface

  * **Fix**: Implement the `Contract` interface on `TokenContract`

* `Error: java: invalid source release 1.9`

  * **Cause**: Your project is compiling against the wrong version of Java

  * **Fix**: Go to `Project Structure > Modules` and change `Language level:` to 8

* `Could not determine java version from 'X.Y.Z'.` when building the nodes from the command line

  * **Cause**: Your terminal is using the wrong version of Java

  * **Fix**: Repoint your terminal to use Java 8 (e.g. "export JAVA_HOME=`/usr/libexec/java_home -v 1.8`" on macOS)

* `Command line is too long. Shorten command line for ---- or also for JUnit default configuration.`

  * **Cause**: You're running the tests through junit which has limitations on the length of the commands you can send

  * **Fix**: Click the Run/Debug configuration drop down > `Edit Configurations...` > `+` > `Gradle`
    In the form that appears, fill in:

    Gradle-Project : `[path to your project]`

    Task: `:cleanTest :test`

    Arguments: `--tests "bootcamp.StateTests"` (or ContractTests or FlowTests)
