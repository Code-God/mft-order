cd ../

echo [INFO]��ʼ����ϴ�˽��...

call mvn clean deploy -pl order-api -am -DskipTests=true

echo [INFO]����ϴ�˽������

pause