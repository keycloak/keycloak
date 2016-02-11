<article>
    <div bs-panel="controls outline">
        <h1 bs-heading>
            <icon icon="{{ctrl.section.icon}}"></icon>
            {{ctrl.section.title}}
        </h1>
    </div>
    <div bs-panel="no-border" ng-if="ctrl.options.mode === 'snippet'">
        <div bs-panel-content="basic">
            <p class="lede">Sorry, Network Throttling is only available in Server or Proxy mode.</p>
        </div>
    </div>
    <div bs-panel="no-border" ng-if="ctrl.options.mode !== 'snippet'">
        <div bs-panel-content="basic">
            <div bs-inputs bs-grid="wide-3 desk-2">
                <div bs-grid-item>
                    <p bs-label-heading>Speed</p>
                    <div bs-input="inline" ng-repeat="(key, item) in ctrl.throttle.targets | orderObjectBy:'order'">
                        <input
                                type="radio"
                                id="speed-{{item.id}}"
                                checked name="speed"
                                ng-model="ctrl.selected"
                                value="{{item.id}}">

                        <label for="speed-{{item.id}}" bs-input-label="light">{{item.title}}</label>
                    </div>
                </div>
                <div bs-grid-item>
                    <p bs-label-heading>Port</p>
                    <div bs-input="text">
                        <div bs-input="inline">
                            <input type="radio" name="port-select" id="port-auto" checked value="auto"
                                   ng-model="ctrl.portEntry">
                            <label for="port-auto" bs-input-label="light">Auto Detection</label>
                        </div>
                        <div bs-input="inline">
                            <input type="radio" id="port-manual" name="port-select" value="manual" ng-model="ctrl.portEntry">
                            <label for="port-manual" bs-input-label="light">User specified <span ng-if="ctrl.state.portError">(between
                                1024 & 65535)</span></label>
                        </div>
                        <input id="server-port"
                               type="text"
                               value=""
                               placeholder="Eg: 1024"
                               ng-model="ctrl.port"
                               ng-focus="ctrl.portEntry = 'manual'"
                               custom-validation>

                    </div>
                    <br/>
                    <div ng-class="[ctrl.state.classname]" bs-state-wrapper>
                        <button
                                id="create-server"
                                bs-button="size-small subtle-alt icon-left"
                                ng-click="ctrl.createServer(ctrl.selected, $event)"
                                ng-disabled="ctrl.state.waiting"
                                >
                            <icon icon="circle-plus"></icon>
                            Create Server
                        </button>
                        <div bs-state-icons>
                            <icon icon="circle-ok"    bs-state="success inline"></icon>
                            <icon icon="circle-minus" bs-state="waiting inline" bs-anim="spin"></icon>
                        </div>
                    </div>
                </div>
                <div bs-grid-item>
                </div>
            </div>
        </div>
        <br/>
        <div bs-panel-content="basic">
            <h3 ng-if="ctrl.serverCount">Your Servers:</h3>
            <h3 ng-if="!ctrl.serverCount">Your Servers will appear here...</h3>
        </div>

        <ul bs-list="bordered inline-controls" bs-offset="basic" id="throttle-server-list">
            <li ng-repeat="(key, item) in ctrl.servers track by key">
                <p bs-width="5">{{$index + 1}}.</p>
                <p bs-width="10"><b>{{item.speed.id | uppercase}}</b></p>
                <p><a href="{{item.urls[0]}}">{{item.urls[0]}}</a></p>
                <p><a href="{{item.urls[1]}}">{{item.urls[1]}}</a></p>
                <div bs-button-group>
                    <button href="#" bs-button="subtle-alt icon" ng-click="ctrl.destroyServer(item, key)">
                        <svg bs-svg-icon><use xlink:href="#svg-bin"></use></svg>
                    </button>
                </div>
            </li>
        </ul>
    </div>

</article>
