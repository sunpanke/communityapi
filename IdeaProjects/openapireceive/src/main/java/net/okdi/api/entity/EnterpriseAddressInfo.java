package net.okdi.api.entity;

public class EnterpriseAddressInfo {
    private Long enterpriseId;

    private Long enterpriseAddressId;

    private Long enterpriseShopId;

    private Long detailedAddresssId;

    private String detailedAddresss;

    private String contactPhone1;

    private String contactPhone2;

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Long enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public Long getEnterpriseAddressId() {
        return enterpriseAddressId;
    }

    public void setEnterpriseAddressId(Long enterpriseAddressId) {
        this.enterpriseAddressId = enterpriseAddressId;
    }

    public Long getEnterpriseShopId() {
        return enterpriseShopId;
    }

    public void setEnterpriseShopId(Long enterpriseShopId) {
        this.enterpriseShopId = enterpriseShopId;
    }

    public Long getDetailedAddresssId() {
        return detailedAddresssId;
    }

    public void setDetailedAddresssId(Long detailedAddresssId) {
        this.detailedAddresssId = detailedAddresssId;
    }

    public String getDetailedAddresss() {
        return detailedAddresss;
    }

    public void setDetailedAddresss(String detailedAddresss) {
        this.detailedAddresss = detailedAddresss;
    }

    public String getContactPhone1() {
        return contactPhone1;
    }

    public void setContactPhone1(String contactPhone1) {
        this.contactPhone1 = contactPhone1;
    }

    public String getContactPhone2() {
        return contactPhone2;
    }

    public void setContactPhone2(String contactPhone2) {
        this.contactPhone2 = contactPhone2;
    }
}