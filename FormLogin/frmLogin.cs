using Guna.UI2.WinForms;
using ResturantManagement.View;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Media;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Xml.Linq;
using System.Data.SqlServerCe;

namespace ResturantManagement.FormLogin
{
    public partial class frmLogin : Form
    {
        SqlCeDataReader dr;
        public frmLogin()
        {
            InitializeComponent();
        }
        WaitFunc wait = new WaitFunc();
        /* public void Alert(string msg, AlertMessage.enmType type)
         {
             AlertMessage alert = new AlertMessage();
             alert.showAlert(msg, type);
         }*/
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        //btn login
        public string username;
        public string _pass = "";
        public string _username = "", _name = "", _role = "";

        private void btnLogin_TextChanged(object sender, EventArgs e)
        { 
            Image img = null;
            try
            {
                bool found;
                ClassConnection.con.Open();
                string qry = "select * from users where username = @username and upass = @password";
                SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
                cm.Parameters.AddWithValue("@username", txtusername.Text);
                cm.Parameters.AddWithValue("@password", txtPass.Text);;

                dr = cm.ExecuteReader();
               // dr.Read();
                if (dr.Read())
                {
                    found = true;

                    _username = dr["username"].ToString();
                    _name = dr["uName"].ToString();
                    _role = dr["uRole"].ToString();
                    _pass = dr["upass"].ToString();
                    Byte[] imageArray = (byte[])(dr["pImage"]);
                    byte[] imageByteArray = imageArray;
                    img = Image.FromStream(new MemoryStream(imageByteArray));
                }
                else
                {
                    found = false;
                }
                dr.Close();
                ClassConnection.con.Close();

                if (found)
                {
                    if (_role == "Cashier" || _role == "Waiter" || _role == "Cleaning" || _role == "Manager" || _role == "Driver" || _role == "Other")
                    {
                        txtusername.Clear();
                        txtPass.Clear();
                        this.Hide();
                        ChangePassword change = new ChangePassword();
                        change.lblUsername.Text = _name;
                        change.lblPass.Text = _pass;

                        wait.Show(this);
                        Thread.Sleep(1000);

                        frmMain main = new frmMain();
                        main.lblRoleName.Text = _name + " | " + _role;
                        main.picUser.Image = img;
                        main.btnStaff.Hide();
                        main.btnAttendance.Hide();
                        main.btnReport.Hide();
                        main.lblCurrency.Hide();
                        main.Show();
                        wait.Close();
                    }

                    else
                    {
                        txtusername.Clear();
                        txtPass.Clear();
                        this.Hide();

                        ChangePassword change = new ChangePassword();
                        change.lblUsername.Text = _name;
                        change.lblPass.Text = _pass;

                        wait.Show(this);
                        Thread.Sleep(1000);

                        frmMain main = new frmMain();
                        main.lblRoleName.Text = _name + " | " + _role;
                        main.picUser.Image = img;
                        main._pass = _pass;
                        main.Show();
                        wait.Close();
                    }
                }
                else
                {
                    //this.Alert("invalid username or passowrd!", AlertMessage.enmType.Error);
                    showToast("ERROR", "invalid username or passowrd!");
                }
            }
            catch (Exception ex)
            {
                ClassConnection.con.Close();
                //this.Alert("Error please try again!", AlertMessage.enmType.Error);
                showToast("ERROR", "Error please try again!");
            }
        }
        //btn close
        private void btnClose_TextChanged(object sender, EventArgs e)
        {
            timer2.Start();
        }
        // btn forgot pass
        private void lblForgotPass_Click(object sender, EventArgs e)
        {
            frmForgotPass frmSend = new frmForgotPass();
            frmSend.Show();
            this.Hide();
        }
        //btn show pass
        private void btnShowPass_Click(object sender, EventArgs e)
        {
            if (txtPass.PasswordChar == '*')
            {
                btnHidePass.BringToFront();
                txtPass.PasswordChar = '\0';
            }
        }
        //btn hide pass
        private void btnHidePass_Click(object sender, EventArgs e)
        {
            if (txtPass.PasswordChar == '\0')
            {
                btnShowPass.BringToFront();
                txtPass.PasswordChar = '*';
            }
        }
        // for enter keypress
        private void txtPass_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (e.KeyChar == 13)
            {
                btnLogin.PerformClick();
            }
        }
        private void timer1_Tick(object sender, EventArgs e)
        {
            if(Opacity == 1)
            {
                timer1.Stop();
            }
            Opacity += .2;
        }
        private void timer2_Tick(object sender, EventArgs e)
        {
            if (Opacity <= 0)
            {
                this.Close();
            }
            Opacity -= .2;
        }

        //btn time login
        private void btnTimeIn_Click(object sender, EventArgs e)
        {
            string TimeIn = DateTime.Now.ToString("h:mm tt");
            string DateIN = DateTime.Now.ToString("dd/MM/yyyy");
            string InStatus = "TimeIn";

            if (txtusername.Text != "" && txtPass.Text != "")
            {
                ClassConnection.con.Open();
                string qry = "UPDATE users set inTime=@inTime,inStatus=@inStatus,inDate=@inDate WHERE username = @username";
                SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
                cm.Parameters.AddWithValue("@username", txtusername.Text);
                cm.Parameters.AddWithValue("@inTime", TimeIn);
                cm.Parameters.AddWithValue("@inStatus", InStatus);
                cm.Parameters.AddWithValue("@inDate", DateIN);
                dr = cm.ExecuteReader();
                ClassConnection.con.Close();
                //this.Alert("Time In has been successfully saved!", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Time In has been successfully saved!.");
            }
            else
            {
                //this.Alert("please input username & password!", AlertMessage.enmType.Error);
                showToast("ERROR", "please input username & password!");
            }
        }
        //btn time logout
        private void btnTimeOut_Click(object sender, EventArgs e)
        {
            string TimeOUT = DateTime.Now.ToString("h:mm tt");
            string DateOUT = DateTime.Now.ToString("dd/MM/yyyy");
            string OutStatus = "TimeOut";
            if (txtusername.Text != "" && txtPass.Text != "")
            {
                ClassConnection.con.Open();
                string qry = "UPDATE users set OutTime=@OutTime,OutStatus=@OutStatus,OutDate=@OutDate WHERE username = @username";
                SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
                cm.Parameters.AddWithValue("@username", txtusername.Text);
                cm.Parameters.AddWithValue("@OutTime", TimeOUT);
                cm.Parameters.AddWithValue("@OutStatus", OutStatus);
                cm.Parameters.AddWithValue("@OutDate", DateOUT);
                dr = cm.ExecuteReader();
                ClassConnection.con.Close();
                //this.Alert("Time In has been successfully saved!", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Time Out has been successfully saved.");
            }
            else
            {
                //this.Alert("please input username & password!", AlertMessage.enmType.Error);
                showToast("ERROR", "please input username & password!");
            }
        }
    }
}
