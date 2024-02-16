import Masthead from "./admin-ui/Masthead";
import SidebarPage from "./admin-ui/SidebarPage";
import TabUtils from "./admin-ui/components/TabPage";
import FormUtils from "./admin-ui/components/FormPage";
import ModalUtils from "../util/ModalUtils";
import ActionToolbarUtils from "./admin-ui/components/ActionToolbarPage";
import TableToolbarUtils from "./admin-ui/components/TableToolbarPage";
import TableUtils from "./admin-ui/components/TablePage";
import EmptyStatePage from "./admin-ui/components/EmptyStatePage";

export default class CommonPage {
  #mastheadPage = new Masthead();
  #sidebarPage = new SidebarPage();
  #tabUtilsObj = new TabUtils();
  #formUtilsObj = new FormUtils();
  #modalUtilsObj = new ModalUtils();
  #actionToolbarUtilsObj = new ActionToolbarUtils();
  #tableUtilsObj = new TableUtils();
  #tableToolbarUtilsObj = new TableToolbarUtils();
  #emptyStatePage = new EmptyStatePage();

  masthead() {
    return this.#mastheadPage;
  }

  sidebar() {
    return this.#sidebarPage;
  }

  tabUtils() {
    return this.#tabUtilsObj;
  }

  formUtils() {
    return this.#formUtilsObj;
  }

  modalUtils() {
    return this.#modalUtilsObj;
  }

  actionToolbarUtils() {
    return this.#actionToolbarUtilsObj;
  }

  tableUtils() {
    return this.#tableUtilsObj;
  }

  tableToolbarUtils() {
    return this.#tableToolbarUtilsObj;
  }

  emptyState() {
    return this.#emptyStatePage;
  }
}
