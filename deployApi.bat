cd ./

echo [INFO]开始提交代码到本地仓库
echo [INFO]当前目录是：%cd%

echo [INFO]开始添加变更
git add -A .
echo [INFO]执行结束！

echo [INFO]提交变更到本地仓库
set /p declation=输入修改:
git commit -m "%declation%"

echo [INFO]拉取远程git服务器代码
git pull origin mall_order_dev

echo [INFO]将变更情况提交到远程git服务器
git push origin mall_order_dev

echo [INFO]批处理执行完毕！！！！


set /p notifyType="是否打包上传私服(y/n)"
if "%notifyType%"=="y" ( 

echo [INFO]开始打包上传私服...

call mvn clean deploy -pl order-api -am -DskipTests=true

echo [INFO]打包上传私服结束

) else (
echo end!
)

pause