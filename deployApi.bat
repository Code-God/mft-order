cd ./

echo [INFO]��ʼ�ύ���뵽���زֿ�
echo [INFO]��ǰĿ¼�ǣ�%cd%

echo [INFO]��ʼ��ӱ��
git add -A .
echo [INFO]ִ�н�����

echo [INFO]�ύ��������زֿ�
set /p declation=�����޸�:
git commit -m "%declation%"

echo [INFO]��ȡԶ��git����������
git pull origin mall_order_dev

echo [INFO]���������ύ��Զ��git������
git push origin mall_order_dev

echo [INFO]������ִ����ϣ�������


set /p notifyType="�Ƿ����ϴ�˽��(y/n)"
if "%notifyType%"=="y" ( 

echo [INFO]��ʼ����ϴ�˽��...

call mvn clean deploy -pl order-api -am -DskipTests=true

echo [INFO]����ϴ�˽������

) else (
echo end!
)

pause