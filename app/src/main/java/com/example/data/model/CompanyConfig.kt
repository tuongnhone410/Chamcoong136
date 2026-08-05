package com.example.data.model

data class CompanyConfig(
    val companyId: String = "default_company",
    val companyName: String = "Công ty Mặc Định",
    val companyCode: String = "DEFAULT",
    val description: String = "",
    val address: String = "",
    
    // Core Salary & Allowance Defaults for this Company
    val luongCoBan: Double = 6000000.0,
    val luongDongBaoHiem: Double = 5000000.0,
    val tiLeDongBaoHiem: Double = 10.5,
    val ngayChotLuong: Int = 1,
    val doanPhiCongDoan: Double = 40000.0,
    
    val heSoOtNgayThuong: Double = 1.5,
    val heSoOtChuNhat: Double = 2.0,
    val heSoOtNgayLe: Double = 3.0,
    val heSoOtDem: Double = 1.75,
    val caDemStart: String = "22:00",
    val caDemEnd: String = "06:00",
    
    val tienChuyenCanGoc: Double = 500000.0,
    val soNgayPhepNam: Int = 12,
    
    // 14 Flat Allowances
    val pcKyThuat: Double = 0.0,
    val pcTrachNhiem: Double = 0.0,
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 30000.0,
    val pcComOt: Double = 15000.0,
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0,
    val pcKhac1: Double = 0.0,
    val pcCaDem: Double = 0.0,
    val allowanceCalcTypes: String = "",
    val soGioNghiGiaiLao: Double = 1.5,
    val tinhKhauTruNghi: Boolean = false,
    val lichTrinh: String = "08:00 - 17:00",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getCalcTypeFor(field: String): String {
        if (allowanceCalcTypes.isBlank()) {
            return UserConfig.getDefaultCalcType(field)
        }
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }
        return map[field] ?: UserConfig.getDefaultCalcType(field)
    }

    fun copyWithCalcType(field: String, calcType: String): CompanyConfig {
        val map = allowanceCalcTypes.split(";").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to parts[1]
        }.toMutableMap()
        map[field] = calcType
        val newStr = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        return this.copy(allowanceCalcTypes = newStr, updatedAt = System.currentTimeMillis())
    }

    fun applyToUserConfig(user: UserConfig, overwriteCustomBaseSalary: Boolean = false): UserConfig {
        return user.copy(
            companyId = this.companyId,
            companyName = this.companyName,
            companyCode = this.companyCode,
            luongCoBan = if (!overwriteCustomBaseSalary && user.luongCoBan > 0.0) user.luongCoBan else this.luongCoBan,
            luongDongBaoHiem = if (!overwriteCustomBaseSalary && user.luongDongBaoHiem > 0.0) user.luongDongBaoHiem else this.luongDongBaoHiem,
            tiLeDongBaoHiem = this.tiLeDongBaoHiem,
            ngayChotLuong = this.ngayChotLuong,
            doanPhiCongDoan = this.doanPhiCongDoan,
            heSoOtNgayThuong = this.heSoOtNgayThuong,
            heSoOtChuNhat = this.heSoOtChuNhat,
            heSoOtNgayLe = this.heSoOtNgayLe,
            heSoOtDem = this.heSoOtDem,
            caDemStart = this.caDemStart,
            caDemEnd = this.caDemEnd,
            tienChuyenCanGoc = this.tienChuyenCanGoc,
            soNgayPhepNam = this.soNgayPhepNam,
            pcKyThuat = this.pcKyThuat,
            pcTrachNhiem = this.pcTrachNhiem,
            pcChucVu = this.pcChucVu,
            pcHieuSuat = this.pcHieuSuat,
            pcSanPham = this.pcSanPham,
            pcComCa = this.pcComCa,
            pcComOt = this.pcComOt,
            pcNhaO = this.pcNhaO,
            pcDocHai = this.pcDocHai,
            pcDtDoanhThu = this.pcDtDoanhThu,
            pcXangXe = this.pcXangXe,
            pcThamNien = this.pcThamNien,
            pcKhac1 = this.pcKhac1,
            pcCaDem = this.pcCaDem,
            allowanceCalcTypes = if (this.allowanceCalcTypes.isNotBlank()) this.allowanceCalcTypes else user.allowanceCalcTypes,
            soGioNghiGiaiLao = this.soGioNghiGiaiLao,
            tinhKhauTruNghi = this.tinhKhauTruNghi,
            lichTrinh = this.lichTrinh
        )
    }

    companion object {
        val DEFAULT_COMPANY = CompanyConfig(
            companyId = "default_company",
            companyName = "Công ty Mặc Định",
            companyCode = "DEFAULT",
            description = "Cấu hình chung mặc định cho toàn bộ người dùng",
            luongCoBan = 6000000.0,
            luongDongBaoHiem = 5000000.0,
            tiLeDongBaoHiem = 10.5,
            ngayChotLuong = 1,
            doanPhiCongDoan = 40000.0,
            heSoOtNgayThuong = 1.5,
            heSoOtChuNhat = 2.0,
            heSoOtNgayLe = 3.0,
            heSoOtDem = 1.75,
            tienChuyenCanGoc = 500000.0,
            pcComCa = 30000.0,
            pcComOt = 15000.0
        )
    }
}
