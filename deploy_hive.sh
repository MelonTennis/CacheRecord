#!/bin/bash

echo start_easy_deploy_hive
cd src
rm -rf ve
mkdir ve
virtualenv ve
source ve/bin/activate
cd qweez
git branch
git checkout release-branch-45
python setup.py install
cd ..
qbol/setup/easy_deploy_hive1_2_vbox.sh
cd ..
cd ..
sudo rm -rf /usr/lib/qubole/packages/hive1_2-None
~/src/qbol/setup/deployer.sh -r -e development -m vbox ~/src/tmp/None/hive1_2-None.tar.gz  localhost
ls -lrt /usr/lib/hive1.2/
deactivate
cd /var/run
sudo mkdir hadoop2
sudo chmod -R 777 hadoop2
/usr/lib/hadoop2/sbin/start-all.sh
