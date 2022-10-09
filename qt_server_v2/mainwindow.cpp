#include "mainwindow.h"

int timeout_limit = 8000;
int gap = 200;
int x_gap = 0;
int y_gap = 0;
int sendco = 0;

QMutex mutex;
QMap<QString,QByteArray> record_map;
QMap<QString,qint64> lastsend_time;
