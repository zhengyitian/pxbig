#ifndef UTIL_H
#define UTIL_H



#include <QPaintEvent>
#include "qtimer.h"
#include "qdebug.h"
#include "qdatetime.h"
#include "qpixmap.h"
#include "qscreen.h"
#include "QPaintEvent"
#include "qpainter.h"
#include "qcursor.h"
#include "QDesktopWidget"
#include "QSettings"
#include "QtNetwork/qtcpsocket.h"
#include "QHBoxLayout"
#include "qpushbutton.h"
#include "qlineedit.h"
#include "qcoreapplication.h"
#include "qmutex.h"
#include "qthread.h"
#include "qdatetime.h"

class para
{
public:
    int xs=1000,ys=100,xl=100,yl=100;
    para(){}
    para(int q,int w,int e,int r){xs=q;ys=w;xl=e;yl=r;}
};

class bu
{
public:
    QByteArray buf;
    int xs=0,ys=0,xl=0,yl=0,si=0;
    bu(){}
    bu(QByteArray da,int a,int b,int c,int d,int e){buf=da;xs=a;ys=b;xl=c;yl=d,si=e;}
};


extern int xCapLen;
extern int yCapLen ;
extern int drawP1;
extern int drawP2 ;
extern int maxX ;
extern int maxY ;
extern QMutex g_lock;
extern para g_para;
extern bu g_buf;


class myLine : public QLineEdit
{
    Q_OBJECT
signals:
    void wh();

public:
    explicit myLine(QString t=""):QLineEdit(t)
    {

    }
    virtual void wheelEvent(QWheelEvent *e)
    {
        int aa = e->angleDelta().y()/120;
        int t = aa+ text().toInt();
        setText(QString::number(t));
        emit(wh());
    }
};

class qthr:public QThread
{
    Q_OBJECT
public:
    int co=0;

    qthr():QThread(){}
    void run()
    {
        while (1) {
            w();
        }
    }

    void w()
    {
        g_lock.lock();
        auto te = g_para;
        g_lock.unlock();
        if(te.xl==0||te.yl==0)
        {
            msleep(1);
            return;
        }
        co++;
        QTcpSocket* s = new QTcpSocket();
        s->connectToHost("10.23.185.230",8899);
        auto xx =    s->waitForConnected(1000);
        if (!xx)
        {
            s->deleteLater();
            sleep(1);
            return;
        }

        QByteArray a;
        a.append((char*)&te.xs,4);
        a.append((char*)&te.ys,4);
        a.append((char*)&te.xl,4);
        a.append((char*)&te.yl,4);
        s->write(a.data(),a.size());
        s->waitForBytesWritten();

        a.clear();
        while (1) {
            s->waitForReadyRead();
            auto x = s->readAll();
            if(x.size()==0)break;
            a.append(x);
        }
        s->deleteLater();
        if(a.size()!=te.xl*te.yl*4)return;

        g_lock.lock();
        g_buf.buf = a;
        g_buf.xs = te.xs;
        g_buf.ys = te.ys;
        g_buf.xl = te.xl;
        g_buf.yl = te.yl;
        g_buf.si = co;
        g_lock.unlock();
    }
};


#endif // UTIL_H
