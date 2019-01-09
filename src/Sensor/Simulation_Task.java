package Sensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import gnu.io.SerialPort;
import utils.SerialPortManager;

/**
 * Simulation_Task 模拟传感器数据发送模块，读取指定传感器数据文件里的数据， 实时给指定串口发送回去。
 * 
 * @author lenovo
 */
public class Simulation_Task implements Runnable
{
    private String           sensorName;    // 模拟器名称
    private SerialPort       port;          // 串口号
    private volatile boolean running = false;// 判断是否要停止发送
    private String           fileName;      // 文件名字
    private int              fileType;      // 文件种类，1为非2进制文件，2为2进制文件


    /**
     * Constructor 用于初始化Simulation_Task
     * 
     * @param sensorName
     *            传感器名称
     * @param port
     *            串口
     * @param fileName
     *            文件名称
     * @param fileType
     *            文件种类
     */
    public Simulation_Task(String sensorName, SerialPort port, String fileName, int fileType)
    {
        this.sensorName = sensorName;
        this.port = port;
        this.fileName = fileName;
        this.fileType = fileType;
    }


    /**
     * Override run method，用于启动数据发送模块
     */
    @Override
    public void run()
    {
        // 判断文件的种类,读取相应的文件
        switch (fileType)
        {
            case 1:
                readNormalFile();
                break;
            case 2:
                readBinaryFile();
                break;
            default:
                return;
        }
    }


    /**
     * 读取非二进制文件，并发送至指定串口。每隔三秒读取并发送一行数据;休眠时间后续可用于模拟器发送数据间隔 （添加休眠参数可模拟部分传感器set
     * interval）
     */
    private void readNormalFile()
    {
        File f = new File(fileName);
        RandomAccessFile scan;
        try
        {
            scan = new RandomAccessFile(f, "r");
            long endpos = f.length();
            while (scan.getFilePointer() != endpos && running)
            {

                String rec_data = ">" + scan.readLine() + "\n";
                // 发送数据至串口
                SerialPortManager.sendToPort(port, rec_data.getBytes());
                Thread.sleep(300);
                // 当该文件被读取至底时，重置读取指针到文件初始位置
                if (scan.getFilePointer() == endpos)
                {
                    scan.seek(0);
                }
            }
            scan.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("No such file");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * 读取二进制文件，并发送至指定串口。每隔三秒读取并发送一个byte
     */
    private void readBinaryFile()
    {
        File f = new File(fileName);
        RandomAccessFile scan;
        try
        {
            scan = new RandomAccessFile(f, "r");
            long endpos = f.length();
            while (scan.getFilePointer() != endpos && running)
            {
                // 读取2进制文件，以16进制发送
                String rec_data = Integer.toHexString(scan.readByte() & 0xff);
                SerialPortManager.sendToPort(port, rec_data.getBytes());
                Thread.sleep(300);
                if (scan.getFilePointer() == endpos)
                {
                    scan.seek(0);
                }
            }
            scan.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("No such file");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * 获取当前数据发送模块状态，是否在运行
     * 
     * @return true 如果在运行中，否则为false
     */
    public boolean getRunningState()
    {
        return this.running;
    }
    /**
     * 获取当前数据发送模块状态，是否在运行
     * 
     * @return true 如果在运行中，否则为false
     */
    public void setRunningState(boolean run)
    {
        this.running=run;
    }


    /**
     * 获取模拟传感器名称
     * 
     * @return 模拟传感器的名称
     */
    public String getsensorName()
    {
        return this.sensorName;
    }


    /**
     * 停止该模块，将运行状态改为false，即可实现停止工作
     */
    public void stop()
    {
        running = false;
    }
}
