package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_config")
data class UserConfig(
    @PrimaryKey val userId: String = "default_user",
    val luongCoBan: Double = 0.0,
    val luongDongBaoHiem: Double = 0.0,
    val tiLeDongBaoHiem: Double = 10.5, // %
    val ngayChotLuong: Int = 30,
    val doanPhiCongDoan: Double = 0.0,
    val heSoOtNgayThuong: Double = 1.5,
    val heSoOtChuNhat: Double = 2.0,
    val heSoOtNgayLe: Double = 3.0,
    val tienChuyenCanGoc: Double = 0.0,
    val soNgayPhepNam: Int = 12,
    val phepNamConLai: Int = 12,
    val lastAccumulatedMonth: String = "",
    val pcKyThuat: Double = 0.0,
    val pcTrachNhiem: Double = 0.0,
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 0.0,
    val pcComOt: Double = 0.0,
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0,
    val pcKhac1: Double = 0.0,
    val pcKhac: Double = 0.0,
    val soGioNghiGiaiLao: Double = 0.0,
    val tinhKhauTruNghi: Boolean = false,
    val hoVaTen: String = "",
    val maNhanVien: String = "",
    val emailDangKy: String = "",
    val ngayVaoLam: String = "",
    val tienComMoiNgay: Double = 0.0,
    val phuCap: Double = 0.0,
    val phuCapXangXe: Double = 0.0,
    val phuCapDienThoai: Double = 0.0,
    val phuCapNhaO: Double = 0.0,
    val phuCapChuyenCan: Double = 0.0,
    val thuong: Double = 0.0,
    val heSoOtDem: Double = 2.0,
    val caDemStart: String = "22:00",
    val caDemEnd: String = "06:00"
)
