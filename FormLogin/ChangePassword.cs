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
using static System.Windows.Forms.VisualStyles.VisualStyleElement.StartPanel;
using System.Data.SqlServerCe;

namespace ResturantManagement.FormLogin
{
    public partial class ChangePassword : Form
    {
        SqlCeDataReader dr;
        public ChangePassword()
        {
            InitializeComponent();
        }

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
        private void ChangePassword_Load(object sender, EventArgs e)
        {
 
        }
        private void picClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }
        private void btnNext_Click(object sender, EventArgs e)
        {
            ClassConnection.con.Open();

            string oldpass = txtOldPass.Text;
            string qry = "select * from users where upass = '" + oldpass + "'";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();

            if (dr.Read() == true)
            {
                //dr.Read();

                txtOldPass.Visible = false;
                btnNext.Visible = false;

                txtNewPass.Visible = true;
                txtComPass.Visible = true;
                btnSave.Visible = true;

                ClassConnection.con.Close();
            }
            else
            {
                //this.Alert("Wrong password, please try again!", AlertMessage.enmType.Error);
                showToast("ERROR", "Wrong password, please try again!");
            }
            dr.Close();
            ClassConnection.con.Close();
        }

        private void btnSave_Click(object sender, EventArgs e)
        {

            string oldpass = txtOldPass.Text;
            string NewPw = txtNewPass.Text;
            string CnfPw = txtComPass.Text;

            // validate input
            if(NewPw == "" || CnfPw == "")
            {
                //this.Alert("please input new password!", AlertMessage.enmType.Error);
                showToast("ERROR", "please input new password!");
                return;
            }
            // check if new password != confirm password
            if (NewPw != CnfPw)
            {
                //this.Alert("New password and confirm\n password  did not matched!", AlertMessage.enmType.Error);
                showToast("ERROR", "New password and confirm password  did not matched!");
            }
            // save new password
            if (NewPw == CnfPw)
            {
                ClassConnection.con.Open();
                string qry = "update users set upass ='" + NewPw + "' where upass = '" + oldpass + "'";
                SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
                cm.ExecuteNonQuery();
                ClassConnection.con.Close();
                //this.Alert("Password has been updated.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Password has been updated.");
                this.Dispose();
            }  
        }
        // enter keypress
        private void ChangePassword_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Escape)
            {
                this.Dispose();
            }
        }

        private void txtOldPass_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (e.KeyChar == 13)
            {
                btnNext.PerformClick();
            }
        }

        private void txtComPass_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (e.KeyChar == 13)
            {
                btnSave.PerformClick();
            }
        }
    }
}
