/* layout: softr-sidebar - one-level-at-a-time hierarchical drill-down */
const { createApp } = Vue;

createApp({
    data() {
        return {
            view: 'HOME',
            domain: {},
            menu: [],

            domains: [],
            layouts: [],
            styles: [],
            selectedDomain: '',
            selectedEntity: null,
            records: [],
            recordsLoading: false,
            settings: {
                appearance: 'LIGHT',
                style: 'softr',
                font: 'Inter',
                contentLayout: 'softr-sidebar'
            }
        };
    },
    computed: {
        entities() {
            return Object.entries(this.domain.entities || {}).map(([id, value]) => ({ id, ...value }));
        },
        relations() {
            return this.domain.relations || [];
        },
        activeEntity() {
            return this.entities.find(entity => entity.id === this.selectedEntity) || null;
        },
        visibleFields() {
            return (this.activeEntity?.fields || []).filter(field => field && field.name);
        },
        appearanceClass() {
            return [
                (this.settings.appearance || 'LIGHT').toLowerCase()
            ];
        },
    },
    methods: {
        openMenu(node) {
            if (node.children && node.children.length > 0) {
                // Parent/module selection is always allowed, even while another
                // module landing page is already visible.
                this.activeMenuGroup = node;
                this.activeMenuGroupId = node.id || node.label;
                this.selectedEntity = null;
                this.records = [];
                this.recordsLoading = false;
                this.view = 'MENU_GROUP';
                return;
            }

            if (node.target) {
                this.selectedEntity = node.target;
                this.records = [];
                this.view = 'ENTITY';
                this.loadRecords(node.target);
            }
        },
        showDashboard() {
            this.selectedEntity = null;
            this.view = 'HOME';
        },
        showUml() {
            this.view = 'UML';
            this.$nextTick(this.drawUml);
        },
        async loadCatalogs() {
            this.domains = await (await fetch('/api/domains')).json();
            this.layouts = await (await fetch('/api/layouts')).json();
        },
        async loadState() {
            const state = await (await fetch('/api/state')).json();
            this.domain = state.domain || {};
            this.menu = state.menu || [];
            this.settings = state.settings || this.settings;
            this.selectedDomain = state.activeFile || '';
        },
        async applyAdmin() {
            await fetch('/api/apply', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    domainFile: this.selectedDomain,
                    settings: this.settings
                })
            });
            sessionStorage.setItem('appModelerViewAfterReload', 'ADMIN');
            window.location.replace('/index.html?v=' + Date.now());
        },
        async loadRecords(entityId) {
            this.recordsLoading = true;
            this.records = [];
            try {
                const response = await fetch('/api/data?entity=' + encodeURIComponent(entityId));
                if (!response.ok) throw new Error('Unable to load records');
                this.records = await response.json();
            } catch (error) {
                console.error('Failed to load entity records:', error);
                this.records = [];
            } finally {
                this.recordsLoading = false;
            }
        },
        displayValue(record, field) {
            const value = record ? record[field.name] : null;
            if (value === null || value === undefined || value === '') return '—';
            if (typeof value === 'boolean') return value ? 'Yes' : 'No';
            if (Array.isArray(value)) return value.join(', ');
            if (typeof value === 'object') return JSON.stringify(value);
            return value;
        },
        drawUml() {
            const svg = document.getElementById('uml');
            if (!svg) return;

            const entities = this.entities;
            const width = Math.max(900, Math.ceil(Math.sqrt(entities.length)) * 220);
            const columns = Math.max(1, Math.floor(width / 220));
            const boxWidth = 180;
            const boxHeight = 105;
            const gapX = 30;
            const gapY = 35;
            const positions = {};

            svg.setAttribute('viewBox', `0 0 ${width} ${Math.ceil(entities.length / columns) * (boxHeight + gapY) + 40}`);
            let output = '';

            entities.forEach((entity, index) => {
                positions[entity.id] = {
                    x: 20 + (index % columns) * (boxWidth + gapX),
                    y: 20 + Math.floor(index / columns) * (boxHeight + gapY)
                };
            });

            this.relations.forEach(relation => {
                const from = positions[relation.fromEntity || relation.from];
                const to = positions[relation.toEntity || relation.to];
                if (from && to) {
                    output += `<line class="rel" x1="${from.x + boxWidth / 2}" y1="${from.y + boxHeight / 2}" x2="${to.x + boxWidth / 2}" y2="${to.y + boxHeight / 2}"/>`;
                }
            });

            entities.forEach(entity => {
                const position = positions[entity.id];
                const fields = (entity.fields || []).slice(0, 5);
                output += `<rect class="entity-box" x="${position.x}" y="${position.y}" rx="8" width="${boxWidth}" height="${boxHeight}"/>`;
                output += `<text class="entity-title" x="${position.x + 9}" y="${position.y + 18}">${this.escape(entity.displayName || entity.id)}</text>`;
                fields.forEach((field, index) => {
                    output += `<text class="entity-field" x="${position.x + 9}" y="${position.y + 37 + index * 13}">${this.escape(field.name)} : ${this.escape(field.type || '')}</text>`;
                });
            });
            svg.innerHTML = output;
        },
        escape(value) {
            return String(value ?? '').replace(/[&<>]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[char]));
        }
    },
    async mounted() {
        await this.loadCatalogs();
        await this.loadState();

        const requestedView = sessionStorage.getItem('appModelerViewAfterReload');
        if (requestedView) {
            this.view = requestedView;
            sessionStorage.removeItem('appModelerViewAfterReload');
        }
    }
}).mount('#app');