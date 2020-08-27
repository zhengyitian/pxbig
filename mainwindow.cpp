#include "mainwindow.h"

int xCapLen = 2244;
int yCapLen = 2244;
int drawP1 = 0;
int drawP2 = 50;
int maxX = 1366;
int maxY = 768;
QMutex g_lock;
para g_para(1000,100,100,100);
bu g_buf(QByteArray(40000,0),100,100,0);


