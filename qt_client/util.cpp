#include "util.h"

int xCapLen = 1366;
int yCapLen = 768;
int drawP1 = 0;
int drawP2 = 50;
int maxX = 1366;
int maxY = 768;
QString g_ip = "127.0.0.1";
QString g_dir = "e:/drawinfo/";
QMutex g_lock;
para g_para(1000,100,100,100);
bu g_buf(QByteArray(40000,0),0,0,100,100,0);
