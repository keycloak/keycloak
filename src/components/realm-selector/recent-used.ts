export class RecentUsed {
  private readonly MAX_NUM = 4; // 3 plus current realm
  private readonly KEY = "recent-used-realms";
  private recentUsedRealms: string[];

  constructor() {
    this.recentUsedRealms = JSON.parse(localStorage.getItem(this.KEY) || "[]");
  }

  private save() {
    this.recentUsedRealms = this.recentUsedRealms.slice(0, this.MAX_NUM);
    localStorage.setItem(this.KEY, JSON.stringify(this.recentUsedRealms));
  }

  clean(existingRealms: string[]) {
    this.recentUsedRealms = this.recentUsedRealms.filter((realm) =>
      existingRealms.includes(realm)
    );
    this.save();
  }

  get used(): string[] {
    return this.recentUsedRealms;
  }

  setRecentUsed(realm: string) {
    if (!this.recentUsedRealms.includes(realm)) {
      this.recentUsedRealms.unshift(realm);
      this.save();
    }
  }
}
