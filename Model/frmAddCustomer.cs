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
using Microsoft.Office.Interop.Excel;
using System.IO;
using System.Web.Security;

namespace ResturantManagement.Model
{
    public partial class frmAddCustomer : Form
    {
        public frmAddCustomer()
        {
            InitializeComponent();
        }
        // this Enable double buffering for all the controls
        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams handleParam = base.CreateParams;
                handleParam.ExStyle |= 0x02000000;
                return handleParam;
            }
        }
        public void showToast(string type, string message)
        {
            ToasForm toas = new ToasForm(type, message);
            toas.Show();
        }
        public string orderType = "";
        public int driverID = 0;
        public string cusName = "";
        public double phone =0;
        public int mainID = 0;
        private void frmAddCustomer_Load(object sender, EventArgs e)
        {
            if(orderType == "Take Away")
            {
                lblDriver.Visible = false;
                cbDriver.Visible = false;
            }
            string qry2 = "Select staffID id, sName name from staff where sRole = 'Driver'";
            ClassConnection.CBFill(qry2, cbDriver);
            if (mainID > 0)
            {
                cbDriver.SelectedValue = driverID;
            }
        }
        private void cbDriver_SelectedIndexChanged(object sender, EventArgs e)
        {
            driverID = Convert.ToInt32(cbDriver.SelectedValue);

            /*ClassConnection.con.Open();
            string qry = "select * from staff where sName =  '" + cbDriver.SelectedItem.ToString() + "' ";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            SqlCeDataAdapter da = new SqlCeDataAdapter(cm);
            System.Data.DataTable dt = new System.Data.DataTable();
            da.Fill(dt);
            foreach(DataRow dr in dt.Rows)
            {
                txtPhoneDriver.Text = dr["sPhone"].ToString();
            }
            ClassConnection.con.Close();*/
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            txtPhone.Text = "";
            txtPhoneDriver.Text = "";
            cbDriver.SelectedIndex = 0;
            cbDriver.SelectedIndex = -1;
            timer1.Start();
        }

        private void btnLogin_Click(object sender, EventArgs e)
        {
            if(cbDriver.SelectedValue == null || txtPhone.Text == "" || txtPhoneDriver.Text == "" || txtAddress.Text == ""){
                //this.Alert("Please select and input value!", AlertMessage.enmType.Warning);
                showToast("WARNING", "Please select and input value!");
            }
            else
            {
                //this.Alert("Informatoin is saved susscessfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Informatoin is saved susscessfully.");
                this.Close();
            }
        }

        private void txtPhoneDriver_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (!char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar))
            {
                e.Handled = true;
            }
        }

        private void txtPhone_KeyPress(object sender, KeyPressEventArgs e)
        {
            if (!char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar))
            {
                e.Handled = true;
            }
        }

        private void timer1_Tick(object sender, EventArgs e)
        {
            if (this.Opacity <= 0)
            {
                this.Opacity -= .2;
            }
            else
            {
                timer1.Stop();
                this.Close();
            }
        }
    }
}
