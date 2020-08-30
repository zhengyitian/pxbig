#ifndef MAINWINDOW_H
#define MAINWINDOW_H
#include "util.h"

class lastDrawInfoC
{
public:
    QByteArray data;int oriXs=0,oriYs=0,oriXl=0,oriYl=0; int id=-1;
    int xs=0,ys=0,xl=0,yl=0,reso=0;
};

class pointItem
{
public:
    int x=0, y=0;
    pointItem(){}
    pointItem(int a,int b){x=a;y=b;}
};

class MainWindow : public QWidget
{
    Q_OBJECT
public:
    qthr * q;
    int xStart = g_para.xs;
    int yStart = g_para.ys;
    int xLen  = g_para.xl;
    int yLen = g_para.yl;
    bool pause = false;
    int reso = 0;
    myLine *l1,*l2,*l3,*l4,*l5;
    QImage lastDrawIm;
    QPushButton *pauseBtn,*setBtn;
    int tipNum=0;
    lastDrawInfoC lastDrawInfo;
    bool hasRepaint = false;
    QLabel* la;
    QMap<int,QVector< pointItem>> drawInfo;

    explicit MainWindow(QWidget *parent = nullptr)
    {
        for(int i=0;i<=300;i++)
            fillDrawinfo(i);
        //     drawInfo[3] = {pointItem(0,0),pointItem(9,9),pointItem(5,5)};
        iniUI();
        QTimer *timer = new QTimer(this);
        connect(timer, SIGNAL(timeout()), this, SLOT(onT()));
        timer->start(20);
        QCoreApplication::instance()->installEventFilter(this);
        q = new qthr();
        q->start();
    }
    void fillDrawinfo(int co)
    {
        auto filename = "e:/drawinfo/"+QString::number(co)+".txt";
        QFile file(filename);
        if(!file.open(QFile::ReadOnly |
                      QFile::Text))
        {
            qDebug() << " Could not open the file for reading "+filename;
            return;
        }
        drawInfo[co] = {};
        QTextStream in(&file);
        while (1) {
            QString myText = in.readLine();
            if(myText == "")
                break;
            auto l = myText.split(" ");
            drawInfo[co].append(pointItem(l[0].toInt()-1,l[1].toInt()-1));
        }
        file.close();
    }
    void iniUI();
    void drawSpec()
    {

    }
    void drawOne(int ty,int v,int i,int j,QImage& im,Qt::GlobalColor col)
    {
        int gap =  reso/3*ty;
        int   redCount = v*reso*reso/3/255;

        if(reso==30&&drawInfo.contains(redCount))
        {
            auto di = i*reso+gap;
            auto dy = j*reso;
            for (int ii=i*reso+gap;ii<gap+i*reso+reso/3;ii++)
            {
                for(int jj=j*reso;jj<(j+1)*reso;jj++)
                {
                    im.setPixelColor(ii,jj,Qt::black);
                }
            }
            for(auto& p:drawInfo[redCount])
            {
                im.setPixelColor(di+p.x,dy+p.y,col);
            }
            return;
        }
        int   redCo = 0;

        for (int ii=i*reso+gap;ii<gap+i*reso+reso/3;ii++)
        {
            for(int jj=j*reso;jj<(j+1)*reso;jj++)
            {
                if( redCo>=redCount)
                {
                    im.setPixelColor(ii,jj,Qt::black);
                    continue;
                }
                im.setPixelColor(ii,jj,col);
                redCo += 1  ;
            }
        }
    }

    void draw(QByteArray&data,int xl,int yl,int xGap=0,int yGap=0)
    {
        QPainter painter(this);
        int  pa = maxX;
        if (xl*reso < maxX)
            pa = xl*reso;
        int pb = maxY;
        if (yl*reso < maxY)
            pb = yl*reso;
        lastDrawIm = QPixmap(QSize(pa,pb)).toImage();
        char* temp = data.data();
        int argb ;

        for (int i=0;i<xl;i++)
        {
            for (int j=0;j<yl;j++)
            {
                if( (i+1)*reso>maxX || (j+1)*reso>maxY)
                    continue;
                int val =  i+xGap+(j+yGap)*(xl+xGap);
                memcpy((char*)&argb,temp+val*4,4);
                drawOne(0,(argb>>0)&0xFF,i,j,lastDrawIm,Qt::red);
                drawOne(1,(argb>>8)&0xFF,i,j,lastDrawIm,Qt::green);
                drawOne(2,(argb>>16)&0xFF,i,j,lastDrawIm,Qt::blue);
            }
        }
        painter.drawImage(QRect(drawP1,drawP2,lastDrawIm.width(),lastDrawIm.height()),lastDrawIm);
    }

    void drawOri()
    {
        lastDrawIm = QPixmap(QSize(lastDrawInfo.oriXl,lastDrawInfo.oriYl)).toImage();
        char* temp = lastDrawInfo.data.data();
        int argb ;
        for(int j=0;j<lastDrawInfo.oriYl;j++)
        {
            for(int i=0;i<lastDrawInfo.oriXl;i++)
            {
                int val =  i+j*lastDrawInfo.oriXl;
                memcpy((char*)&argb,temp+val*4,4);
                lastDrawIm.setPixelColor(i,j,QColor( (argb>>0)&0xFF, (argb>>8)&0xFF, (argb>>16)&0xFF));
            }
        }
        QPainter painter(this);
        painter.drawImage(QRect(drawP1,drawP2,lastDrawIm.width(),lastDrawIm.height()),lastDrawIm);
    }

    void  paintEvent(QPaintEvent* e)
    {
        QWidget::paintEvent(e);
        if (!needRepaint())
        {
            QPainter painter(this);
            painter.drawImage(QRect(drawP1,drawP2,lastDrawIm.width(),lastDrawIm.height()),lastDrawIm);
            return ;
        }

        lastDrawInfo.xs=xStart;lastDrawInfo.ys=yStart;lastDrawInfo.xl = xLen;lastDrawInfo.yl=yLen;lastDrawInfo.reso=reso;
        if(!pause)
        {
            g_lock.lock();
            lastDrawInfo.data=g_buf.buf;
            lastDrawInfo.oriXl = g_buf.xl;
            lastDrawInfo.oriYl = g_buf.yl;
            lastDrawInfo.oriXs = g_buf.xs;
            lastDrawInfo.oriYs = g_buf.ys;
            lastDrawInfo.id = g_buf.si;
            g_lock.unlock();
            la->setNum(lastDrawInfo.id);
            if(reso==0)
            {
                return   drawOri();
            }
            return   draw(lastDrawInfo.data,lastDrawInfo.oriXl,lastDrawInfo.oriYl);
        }

        if(reso==0)
        {
            return  drawOri();
        }

        auto xgap = xStart-lastDrawInfo.oriXs;
        auto ygap = yStart-lastDrawInfo.oriYs;
        if (xgap<0)xgap=0;
        if(ygap<0)ygap=0;

        if(xgap>lastDrawInfo.oriXl)xgap = lastDrawInfo.oriXl;
        if(ygap>lastDrawInfo.oriYl)xgap = lastDrawInfo.oriYl;
        return   draw(lastDrawInfo.data,lastDrawInfo.oriXl-xgap,lastDrawInfo.oriYl-ygap,xgap,ygap);

    }

    bool eventFilter(QObject *obj, QEvent *e);
    void setErr();
    void setOk();
    void setPara()
    {
        int xx=xLen; int yy=yLen;
        if(reso>0)
        {
            if(xx>maxX/reso)xx=maxX/reso;
            if(yy>maxY/reso)yy=maxY/reso;
        }
        g_lock.lock();
        if(pause)
        {
            g_para = para(0,0,0,0);
        }
        else
            g_para = para(xStart,yStart,xx,yy);
        g_lock.unlock();
    }

    bool needRepaint()
    {
        if(pause)
        {
            if(xStart==lastDrawInfo.xs&&yStart==lastDrawInfo.ys&&reso==lastDrawInfo.reso)
                return  false;
            hasRepaint = false;
            return true;
        }

        bool j=true;
        g_lock.lock();
        if(lastDrawInfo.id==g_buf.si)
        {
            j = false;
        }
        g_lock.unlock();
        return j;
    }

public slots:

    void onT()
    {
        setPara();
        if(!needRepaint())
        {
            if (hasRepaint)
            {
                return;
            }
            hasRepaint = true;
        }
        update();
    }

    void OnP();
    void onCli();
};

#endif
