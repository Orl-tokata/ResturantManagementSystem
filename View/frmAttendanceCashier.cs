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

namespace ResturantManagement.View
{
    public partial class frmAttendanceCashier : Form
    {
        SqlCeDataReader dr;
        public frmAttendanceCashier()
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
        public void LoadAttendance()
        {
            int i = 0;
            dgvAttendance.Rows.Clear();
            ClassConnection.con.Open();
            string qry = "SELECT * from users where username like '%" + txtSearch.Text + "%'";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            while (dr.Read())
            {
                i++;
                dgvAttendance.Rows.Add(i, dr["username"].ToString(), dr["upass"].ToString(), dr["uphone"].ToString(), dr["uName"].ToString(), dr["inTime"].ToString(), dr["inStatus"].ToString(), dr["inDate"].ToString(), dr["OutTime"].ToString(), dr["OutStatus"].ToString(), dr["OutDate"].ToString());
            }
            dr.Close();
            ClassConnection.con.Close();
        }

        private void btnTimeIN_Click(object sender, EventArgs e)
        {
            int i = 0;
            dgvAttendance.Rows.Clear();
            ClassConnection.con.Open();
            string qry = "Select * From users";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            while (dr.Read())
            {
                i++;
                dgvAttendance.Rows.Add(i, dr["username"].ToString(), dr["upass"].ToString(), dr["uName"].ToString(), dr["uphone"].ToString(), dr["inTime"].ToString(), dr["inStatus"].ToString(), dr["inDate"].ToString());
            }
            dr.Close();
            ClassConnection.con.Close();
        }

        private void btnTimeOut_Click(object sender, EventArgs e)
        {
            int i = 0;
            dgvAttendance.Rows.Clear();
            ClassConnection.con.Open();
            string qry = "Select * From users";
            SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
            dr = cm.ExecuteReader();
            while (dr.Read())
            {
                i++;
                dgvAttendance.Rows.Add(i, dr["username"].ToString(), dr["upass"].ToString(), dr["uName"].ToString(), dr["uphone"].ToString(), dr["inTime"].ToString(), dr["inStatus"].ToString(), dr["inDate"].ToString(), dr["OutTime"].ToString(), dr["OutStatus"].ToString(), dr["OutDate"].ToString());
            }
            dr.Close();
            ClassConnection.con.Close();
        }

        private void dgvAttendance_CellContentClick(object sender, DataGridViewCellEventArgs e)
        {
            string colName = dgvAttendance.Columns[e.ColumnIndex].Name;
            if (colName == "Delete")
            {
                if (MessageBox.Show("Are you sure you want to delete this record?", "Delete Record", MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes)
                {
                    ClassConnection.con.Open();
                    string qry = "DELETE FROM users WHERE username LIKE '" + dgvAttendance[1, e.RowIndex].Value.ToString() + "'";
                    SqlCeCommand cm = new SqlCeCommand(qry, ClassConnection.con);
                    cm.ExecuteNonQuery();
                    ClassConnection.con.Close();
                    //this.Alert("Attendance Cashier has been successfully deleted.", AlertMessage.enmType.Success);
                    showToast("SUCCESS", "Attendance Cashier has been successfully deleted.");
                }
            }
            LoadAttendance();
        }

        private void txtSearch_TextChanged(object sender, EventArgs e)
        {
            LoadAttendance();
        }
    }
}
