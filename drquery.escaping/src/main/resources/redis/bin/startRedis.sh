./redis-server ../conf/redis.conf
sleep 10
source ~/.bash_profile
cd /home_app/drcache/sync/bin
./sync.sh
