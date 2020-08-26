#ifndef MAINWINDOW_H
#define MAINWINDOW_H

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
#include "qnetwork.h"
#include "QtNetwork/qtcpsocket.h"
#include "QHBoxLayout"
#include "qpushbutton.h"
#include "qlineedit.h"
#include "qcoreapplication.h"
#include "qmutex.h"
#include "qthread.h"

class para
{
public:
    int a=1000,b=100,c=100,d=100;
    para(){}
    para(int q,int w,int e,int r){a=q;b=w;c=e;d=r;}
};
class bu
{
public:
    QByteArray buf;
    int x,y,si;
    bu(){}
    bu(QByteArray a,int b,int c,int d){buf=a;x=b;y=c,si=d;}
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
        co++;
        QTcpSocket* s = new QTcpSocket();
        s->connectToHost("10.23.185.230",8899);
        s->waitForConnected();
        g_lock.lock();
        auto te = g_para;
        g_lock.unlock();
        QByteArray a;
        a.append((char*)&te.a,4);
        a.append((char*)&te.b,4);
        a.append((char*)&te.c,4);
        a.append((char*)&te.d,4);
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
        if(a.size()!=te.c*te.d*4)return;

        g_lock.lock();
        g_buf.buf = a;
        g_buf.x = te.c;
        g_buf.y = te.d;
        g_buf.si = co;
        g_lock.unlock();
    }
};


class MainWindow : public QWidget
{
    Q_OBJECT
public:

    qthr * q;
    int xStart = g_para.a;
    int yStart = g_para.b;
    int xLen  = g_para.c;
    int yLen = g_para.d;
    bool pause = false;
    int reso = 0;
    myLine *l1,*l2,*l3,*l4,*l5;
    int lastDrawId = -1;
    int lastDrawReso = 0;
QImage lastDrawIm;
    explicit MainWindow(QWidget *parent = nullptr)
    {

        QTimer *timer = new QTimer(this);
        connect(timer, SIGNAL(timeout()), this, SLOT(onT()));
        timer->start(50);
        QCoreApplication::instance()->installEventFilter(this);

        QVBoxLayout *v = new QVBoxLayout();
        QHBoxLayout *h = new QHBoxLayout();

        QPushButton *p = new QPushButton("set");
        connect(p, SIGNAL(clicked()), this, SLOT(onCli()));

        l1 = new myLine(QString::number(xStart));
        l2 = new myLine(QString::number(yStart));
        l3 = new myLine(QString::number(xLen));
        l4 = new myLine(QString::number(yLen));
        l5 = new myLine(QString::number(reso));
        connect(l1, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l2, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l3, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l4, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l5, SIGNAL(wh()), this, SLOT(onCli()));

        h->addWidget(l1);
        h->addWidget(l2);
        h->addWidget(l3);
        h->addWidget(l4);
        h->addWidget(l5);
        h->addWidget(p);
        v->addLayout(h);
        v->addStretch();
        this->setLayout(v);
        q = new qthr();
        q->start();

    }

    void drawOne(int ty,int v,int i,int j,QImage& im)
    {
        int   redCount = v*reso*reso/3/255;
        int   redCo = 0;
        int gap =  reso/3*ty;
        for (int ii=i*reso+gap;ii<gap+i*reso+reso/3;ii++)
        {
            for(int jj=j*reso;jj<(j+1)*reso;jj++)
            {
                if( redCo>=redCount)
                {
                    im.setPixelColor(ii,jj,Qt::black);
                    continue;
                }
                if(ty==0)
                {
                    im.setPixelColor(ii,jj,Qt::red);
                }
                else if(ty ==1)
                {
                    im.setPixelColor(ii,jj,Qt::green);
                }
                else if(ty==2)
                {
                    im.setPixelColor(ii,jj,Qt::blue);
                }
                redCo += 1  ;
            }
        }
    }

    void draw(QByteArray&data,int xl,int yl)
    {
        QPainter painter(this);
        int  pa = maxX;
        if (xl*reso < maxX)
            pa = xl*reso;
        int pb = maxY;
        if (yl*reso < maxY)
            pb = yl*reso;
        auto im = QPixmap(QSize(pa,pb)).toImage();
        char* temp = data.data();
        int argb ;

        for (int i=0;i<xl;i++)
        {
            for (int j=0;j<yl;j++)
            {
                if( (i+1)*reso>maxX || (j+1)*reso>maxY)
                    continue;
                int val =  i+j*xl;
                memcpy((char*)&argb,temp+val*4,4);
                drawOne(0,(argb>>0)&0xFF,i,j,im);
                drawOne(1,(argb>>8)&0xFF,i,j,im);
                drawOne(2,(argb>>16)&0xFF,i,j,im);
            }
        }
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im);
        lastDrawIm = im;
    }

    void drawOri(QByteArray&data,int xL,int yL)
    {
        auto im = QPixmap(QSize(xL,yL)).toImage();
        char* temp = data.data();
        int argb ;
        for(int j=0;j<yL;j++)
        {
            for(int i=0;i<xL;i++)
            {
                int val =  i+j*xL;
                memcpy((char*)&argb,temp+val*4,4);
                im.setPixelColor(i,j,QColor( (argb>>0)&0xFF, (argb>>8)&0xFF, (argb>>16)&0xFF));
            }
        }

        QPainter painter(this);
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im);
        lastDrawIm = im;
    }
    void  paintEvent(QPaintEvent* e)
    {
        g_lock.lock();
        g_para = para(xStart,yStart,xLen,yLen);
        auto da=g_buf.buf;auto xLenB = g_buf.x;auto yLenB = g_buf.y;int si=g_buf.si;
        g_lock.unlock();
        if(lastDrawId==si && lastDrawReso==reso)
        {
              QPainter painter(this);
                painter.drawImage(QRect(drawP1,drawP2,lastDrawIm.width(),lastDrawIm.height()),lastDrawIm);
        }

        lastDrawId = si;
        lastDrawReso = reso;
        if(reso==0) return drawOri(da,xLenB,yLenB);
        draw(da,xLenB,yLenB);
    }

    bool eventFilter(QObject *obj, QEvent *e)
    {
        if (e->type()==QEvent::KeyPress )
        {
            QKeyEvent *keyEvent = static_cast<QKeyEvent*>(e);

            if(keyEvent->key()==65)
                l1->setText(QString::number(l1->text().toInt()-10));

            else if(keyEvent->key()==74)
                l3->setText(QString::number(l3->text().toInt()-10));

            else if(keyEvent->key()==87)
                l2->setText(QString::number(l2->text().toInt()-10));

            else if(keyEvent->key()==73)
                l4->setText(QString::number(l4->text().toInt()-10));

            else if(keyEvent->key()==75)
                l4->setText(QString::number(l4->text().toInt()+10));

            else if(keyEvent->key()==76)
                l3->setText(QString::number(l3->text().toInt()+10));

            else if(keyEvent->key()==83)
                l2->setText(QString::number(l2->text().toInt()+10));

            else if(keyEvent->key()==81)
                l5->setText(QString::number(l5->text().toInt()-3));

            else if(keyEvent->key()==69)
                l5->setText(QString::number(l5->text().toInt()+3));

            else if(keyEvent->key()==68)
                l1->setText(QString::number(l1->text().toInt()+10));

            else
            {
                return QWidget::eventFilter( obj, e );
            }
            onCli();
            e->accept();
            return true;
        }
    }

public slots:
    void onT()
    {
        repaint();
    }


    void onCli()
    {
        int   xS2 =  l1->text().toInt();
        int yS2 =  l2->text().toInt();
        int   xL2 =  l3->text().toInt();
        int  yL2 =  l4->text().toInt();
        reso =  l5->text().toInt();
        if (reso<0) reso=0;
        reso = int(reso/3)*3;
        if (xS2<0||yS2<0||xL2<=0||yL2<=0)
            return;

        if (xS2+xL2>xCapLen||yS2+yL2>yCapLen)
            return;
        xStart = xS2;yStart=yS2;xLen = xL2;yLen = yL2;
    }

};

#endif
