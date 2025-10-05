using Microsoft.ReportingServices.ReportProcessing.ReportObjectModel;
using ResturantManagement.View;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SqlClient;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.StartPanel;

namespace ResturantManagement.FormLogin
{
    public partial class frmForgotPass : Form
    {
        public frmForgotPass()
        {
            InitializeComponent();
        }
        public string id = "";
        /*public void Alert(string msg, AlertMessage.enmType type)
        {
            AlertMessage alert = new AlertMessage();
            alert.showAlert(msg, type);
        }*/
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        private void btnSendName_Click(object sender, EventArgs e)
        {
            ClassConnection.con.Open();
            string qry = "select * from users where username = '" + txtUsername.Text + "'";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataReader dr = cm.ExecuteReader();
            if (txtUsername.Text != "")
            {
                if (dr.Read())
                {
                    txtShowPass.Text = dr.GetValue(2).ToString();
                    //MessageBox.Show("pass : " + txtShowPass.Text);
                }
                else
                {
                    //this.Alert("Username not match!", AlertMessage.enmType.Warning);
                    showToast("WARNING", "Username not match!");
                }
                dr.Close();
                ClassConnection.con.Close();
            }
            else
            {
                //this.Alert("please input username!", AlertMessage.enmType.Error);
                showToast("ERROR", "please input username!");
            }
            ClassConnection.con.Close();
        }
        private void picBack_Click(object sender, EventArgs e)
        {
            frmLogin frmLogin = new frmLogin();
            frmLogin.Show();
            this.Hide();
        }
    }
}
