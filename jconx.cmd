@echo off
start /B /D c:\temp jconsole -pluginpath c:\libs\java\jconsole-plugins\topthreads-1.1.jar -J-Dcom.sun.tools.jconsole.mbeans.keyPropertyList="" -J-Djava.class.path=%JAVA_HOME%\lib\jconsole.jar;c:\users\nwhitehe\.m2\repository\com\sun\jdmk\jmx-optional\1.0-b02-SNAPSHOT\jmx-optional-1.0-b02-SNAPSHOT.jar service:jmx:jmxmp://localhost:%1
