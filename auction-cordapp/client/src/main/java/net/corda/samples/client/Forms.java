package net.corda.samples.client;

public class Forms {

    public static class BidForm {
        private String auctionId;
        private int amount;

        public String getAuctionId() {
            return auctionId;
        }

        public void setAuctionId(String auctionId) {
            this.auctionId = auctionId;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    public static class SettlementForm {
        private String auctionId;
        private String amount;

        public String getAuctionId() {
            return auctionId;
        }

        public void setAuctionId(String auctionId) {
            this.auctionId = auctionId;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }
    }

    public static class CreateAuctionForm {
        private int basePrice;
        private String assetId;
        private String deadline;

        public int getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(int basePrice) {
            this.basePrice = basePrice;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }
    }

    public static class IssueCashForm {
        private String party;
        private int amount;

        public String getParty() {
            return party;
        }

        public void setParty(String party) {
            this.party = party;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    public static class AssetForm {
        private String imageUrl;
        private String title;
        private String description;

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

}
