#include "util.h"

//config before compile

//biggest size the server can capture
int xCapLen = 2244;
int yCapLen = 2244;

//the screen size the client has
int maxX = 1366;
int maxY = 768;

//server ip
//QString g_ip = "192.168.1.110";
//QString g_ip = "127.0.0.1";
QString g_ip = "192.168.43.1";
//drawinfo dir
QString g_dir = "e:/drawinfo/";

//end of config

int drawP1 = 0;
int drawP2 = 50;
QMutex g_lock;
para g_para(1000,100,100,100);
bu g_buf(QByteArray(40000,0),0,0,100,100,0);
