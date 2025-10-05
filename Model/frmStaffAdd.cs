using ResturantManagement.View;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data.SqlServerCe;


namespace ResturantManagement.Model
{
    public partial class frmStaffAdd : Form
    {
        public frmStaffAdd()
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
        public int id = 0;

        private void btnSave_Click(object sender, EventArgs e)
        {
            string qry = "";
            if(txtName.Text == "" || txtPhone.Text == "" || cbRole.SelectedItem == null)
            {
                // MessageBox.Show("please input details! ");
                //this.Alert("please input details!", AlertMessage.enmType.Warning);
                showToast("WARNING", "please input details!");
                return;
            }
            if (id == 0) // insert
            {
                qry = "insert into staff (sName,sPhone,sRole) values(@Name,@phone,@role)";
            }
            else // update
            {
                qry = "update staff set sName = @Name, sPhone = @phone, sRole = @role where staffID = @id ";
            }

            Hashtable ht = new Hashtable();
            ht.Add("@id", id);
            ht.Add("@Name", txtName.Text);
            ht.Add("@phone", txtPhone.Text);
            ht.Add("@role", cbRole.Text);

            if (ClassConnection.SQl(qry, ht) > 0)
            {
                //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Saved Successfully.");
                id = 0;
                txtName.Clear();
                txtPhone.Clear();
                cbRole.SelectedIndex = -1;
                txtName.Focus();
            }
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
        }
        // validate input number only
        private void txtPhone_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar);
        }
    }
}
