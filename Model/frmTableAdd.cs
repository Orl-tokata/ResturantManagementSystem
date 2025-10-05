using ResturantManagement.View;
using System;
using System.Collections;
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

namespace ResturantManagement.Model
{
    public partial class frmTableAdd : Form
    {
        public frmTableAdd()
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

        private void btnSave_Click_1(object sender, EventArgs e)
        {
            string qry = "";
            if (txtName.Text == "" && cbStatus.SelectedValue == null)
            {
                //this.Alert("please input Table name!", AlertMessage.enmType.Warning);
                showToast("WARNING", "please input Table name or Status!");
                //return; 
            }
            else
            {
                if (id == 0) // insert
                {
                    qry = "insert into tables (tname,status) values(@Name,@status)";
                }
                else // update
                {
                    qry = "update tables set tName = @Name, status=@status where tID = @id ";
                }
                Hashtable ht = new Hashtable();
                ht.Add("@id", id);
                ht.Add("@Name", txtName.Text);
                ht.Add("@status", cbStatus.Text);

                if (ClassConnection.SQl(qry, ht) > 0)
                {
                    //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Saved Successfully.");
                    id = 0;
                    txtName.Clear();
                    cbStatus.SelectedIndex = -1;
                    txtName.Focus();
                }
            }
            /*SqlCeCommand cmd = new SqlCeCommand(qry, ClassConnection.con);
           
            cmd.Parameters.AddWithValue("@id", id);
            cmd.Parameters.Add("@Name", SqlDbType.NVarChar, 50);
            cmd.Parameters["@Name"].Value = txtName.Text;
            cmd.Parameters.AddWithValue("@status", Convert.ToInt32(cbStatus.SelectedValue));

            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            if (id > 0)
            {
                id = Convert.ToInt32(cmd.ExecuteScalar());
                //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Saved Successfully.");
                txtName.Focus();
                txtName.Clear();

                cbStatus.SelectedIndex = 0;
                cbStatus.SelectedIndex = -1;
                ClassConnection.con.Close();
            }
            else
            {
                id = Convert.ToInt32(cmd.ExecuteScalar());
                //this.Alert("Saved Successfully.", AlertMessage.enmType.Success);
                showToast("SUCCESS", "Saved Successfully.");
                txtName.Focus();
                txtName.Clear();
                cbStatus.SelectedIndex = 0;
                cbStatus.SelectedIndex = -1;
                ClassConnection.con.Close();
            }*/
        }

        private void btnClose_Click_1(object sender, EventArgs e)
        {
            this.Close();
        }
    }
}
