echo off

echo [INFO]请输入需要升级的版本:

set /p newVersion= 

echo [INFO]输入%newVersion% 开始替换版本
set var= ..\
cd %var%
call mvn clean versions:set -DnewVersion=%newVersion%

echo [INFO]所有子模块都升级版本成功 新版本为%newVersion%，是否上传jar包至私服

pause

echo [INFO]开始打包上传私服...

call mvn clean deploy -DskipTests=true

echo [INFO]打包上传私服结束

pause