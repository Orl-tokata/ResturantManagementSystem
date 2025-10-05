using ResturantManagement.Model;
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

namespace ResturantManagement.View
{
    public partial class frmUserStaffInfo : Form
    {
        SqlCeDataReader dr;
        public frmUserStaffInfo()
        {
            InitializeComponent();
        }
        private void frmUserStaffInfo_Load(object sender, EventArgs e)
        {
            GetDataSatff();
            DataUser();

            lblMgnStaff.Show();
            lblMgnUser.Hide();

            lblStaff.Show();
            lblTotalStaff.Show();
            totalUser.Hide();
            lblUser.Hide();
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
        // load data staff
        public void GetDataSatff()
        {
            string qry = "select * from staff where sName like '%" + txtSearch.Text + "%'";
            ListBox lb = new ListBox();
            lb.Items.Add(dgvid);
            lb.Items.Add(dgvName);
            lb.Items.Add(dgvPhone);
            lb.Items.Add(dgvRole);

            ClassConnection.LoadData(qry, guna2DataGridView1, lb);

            //TODO: count staff
            lblTotalStaff.Text = countStaff("select count(*) from staff").ToString();//+ " Table ";
        }
        public double countStaff(string sql)
        {
            ClassConnection.con.Open();
            SqlCeCommand cm = new SqlCeCommand(sql, ClassConnection.con);
            double data = double.Parse(cm.ExecuteScalar().ToString());
            ClassConnection.con.Close();
            return data;
        }
        // load form add Staff
        private void btnAdd_Click(object sender, EventArgs e)
        {
            ClassConnection.BlueBackground(new Model.frmStaffAdd());
            GetDataSatff();
        }

        private void txtSearch_TextChanged(object sender, EventArgs e)
        {
            GetDataSatff();
        }

        private void guna2DataGridView1_CellContentClick(object sender, DataGridViewCellEventArgs e)
        {
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvedit")
            {
                // it is change as we have to set form text propties befor open
                frmStaffAdd frmAdd = new frmStaffAdd();
                frmAdd.id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                frmAdd.txtName.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvName"].Value);
                frmAdd.txtPhone.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvPhone"].Value);
                frmAdd.cbRole.Text = Convert.ToString(guna2DataGridView1.CurrentRow.Cells["dgvRole"].Value);
                ClassConnection.BlueBackground(frmAdd);
                GetDataSatff();

            }

            // need to confirm befor delete data
            if (guna2DataGridView1.CurrentCell.OwningColumn.Name == "dgvdel")
            {
                guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;

                if (guna2MessageDialog1.Show("Are you sure want to delete?") == DialogResult.Yes)
                {
                    int id = Convert.ToInt32(guna2DataGridView1.CurrentRow.Cells["dgvid"].Value);
                    string qry = "Delete from staff where staffID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    ClassConnection.SQl(qry, ht);

                    //this.Alert("Deleted Successfull.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Deleted Successfull.");
                    GetDataSatff();
                }
            }
        }
        // load data user
        public void DataUser()
        {
            int i = 0;
            dgvUserInfo.Rows.Clear();
            if (ClassConnection.con.State == ConnectionState.Closed) { ClassConnection.con.Open(); }
            string qry = "select * from users where uName like '%" + txtSearchUser.Text + "%'";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            while (dr.Read())
            {
                i++;
                dgvUserInfo.Rows.Add(i, dr["userID"].ToString(), dr["username"].ToString(), dr["uphone"].ToString(), dr["uName"].ToString(), dr["upass"].ToString(), dr["uRole"].ToString());
            }
            dr.Close();
            ClassConnection.con.Close();

            //TODO: count user
            totalUser.Text = countStaff("select count(*) from users").ToString();//+ " Table ";
        }
 
        private void btnAddUser_Click(object sender, EventArgs e)
        {
            ClassConnection.BlueBackground(new Model.frmUserAdd());
            DataUser();
        }

        private void txtSearchUser_TextChanged(object sender, EventArgs e)
        {
            DataUser();
        }

        private void guna2DataGridView2_CellContentClick(object sender, DataGridViewCellEventArgs e)
        {
            if (dgvUserInfo.CurrentCell.OwningColumn.Name == "editUser")
            {
                // it is change as we have to set form text propties befor open
                frmUserAdd frmAdd = new frmUserAdd();
                frmAdd.id = Convert.ToInt32(dgvUserInfo.CurrentRow.Cells["dgvUserid"].Value);
                ClassConnection.BlueBackground(frmAdd);
                DataUser();

            }

            // need to confirm befor delete data
            if (dgvUserInfo.CurrentCell.OwningColumn.Name == "deluser")
            {
                guna2MessageDialog1.Icon = Guna.UI2.WinForms.MessageDialogIcon.Question;
                guna2MessageDialog1.Buttons = Guna.UI2.WinForms.MessageDialogButtons.YesNo;

                if (guna2MessageDialog1.Show("Are you sure want to delete?") == DialogResult.Yes)
                {
                    int id = Convert.ToInt32(dgvUserInfo.CurrentRow.Cells["dgvUserid"].Value);
                    string qry = "Delete from users where userID = '" + id + "'";
                    Hashtable ht = new Hashtable();
                    ClassConnection.SQl(qry, ht);

                    //this.Alert("Deleted Successfull.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Deleted Successfull.");
                    DataUser();
                }
            }
        }

        private void metroTabControl1_SelectedIndexChanged(object sender, EventArgs e)
        {
            if(metroTabControl1.SelectedTab == metroTabControl1.TabPages["staff"])
            {
                lblMgnStaff.Show();
                lblMgnUser.Hide();

                lblStaff.Show();
                lblTotalStaff.Show();
                totalUser.Hide();
                lblUser.Hide();
                GetDataSatff();
            }
            else if(metroTabControl1.SelectedTab == metroTabControl1.TabPages["user"])
            {
                lblMgnStaff.Hide();
                lblMgnUser.Show();

                lblStaff.Hide();
                lblTotalStaff.Hide();
                totalUser.Show();
                lblUser.Show();
                DataUser();
            }
        }
    }
}
