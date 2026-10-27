<script setup lang="ts">
import { computed, ref } from 'vue';
import type { MoveAnalysis } from '@/models/analysis';
import { sourceLabel } from '@/models/coaching';
import CandidateMoves from './CandidateMoves.vue';
import StrategicFindings from './StrategicFindings.vue';

const props = defineProps<{ analysis: MoveAnalysis; selectedCandidate: string | null }>();
const emit = defineEmits<{ candidate: [uci: string]; highlight: [squares: string[]]; deepen: []; reanalyse: [] }>();

const tab = ref('explanation');
const tactical = computed(() => (props.analysis.findings ?? []).filter((f) => f.category === 'TACTICAL'));
const strategic = computed(() => (props.analysis.findings ?? []).filter((f) => f.category === 'STRATEGIC' && f.ruleId !== 'STRAT-011'));
const userPlans = computed(() => (props.analysis.strategicPlans ?? []).filter((p) => p.side === 'USER'));
const opponentPlans = computed(() => (props.analysis.strategicPlans ?? []).filter((p) => p.side === 'OPPONENT'));
</script>

<template>
  <q-card flat bordered class="coaching-details" data-testid="coaching-details">
    <q-tabs v-model="tab" dense align="left" class="text-primary" narrow-indicator inline-label outside-arrows mobile-arrows>
      <q-tab name="explanation" icon="school" label="Erklärung" no-caps />
      <q-tab name="candidates" icon="list" label="Kandidaten" no-caps data-testid="tab-candidates" />
      <q-tab name="findings" icon="search" label="Befunde" no-caps />
      <q-tab name="plans" icon="flag" label="Pläne" no-caps />
      <q-tab name="engine" icon="memory" label="Engine" no-caps />
    </q-tabs>
    <q-separator />
    <q-tab-panels v-model="tab" animated keep-alive>
      <q-tab-panel name="explanation">
        <div v-for="section in analysis.sections ?? []" :key="section.key" class="q-mb-md">
          <div class="text-subtitle2">{{ section.question }}</div>
          <ul class="q-pl-md q-my-xs">
            <li v-for="(s, i) in section.statements" :key="i" class="text-body2">
              {{ s.text }}
              <span v-if="s.sources.length" class="sources">[{{ s.sources.map(sourceLabel).join(', ') }}]</span>
            </li>
          </ul>
        </div>
        <q-card v-if="analysis.tip" flat class="bg-amber-1 q-pa-sm">
          <div class="text-subtitle2"><q-icon name="tips_and_updates" /> Merksatz</div>
          <div class="text-body2">{{ analysis.tip.learningPrinciple }}</div>
        </q-card>
      </q-tab-panel>

      <q-tab-panel name="candidates">
        <CandidateMoves :candidates="analysis.candidates ?? []" :selected-uci="selectedCandidate" @select="emit('candidate', $event)" />
      </q-tab-panel>

      <q-tab-panel name="findings">
        <StrategicFindings :findings="tactical" title="Taktische Motive (Ebene 1)" empty-text="Keine taktischen Motive erkannt."
                           @highlight="emit('highlight', $event)" />
        <div class="q-mt-md" />
        <StrategicFindings :findings="strategic" title="Strategische Bewertung (Ebene 2, nach Lasker)"
                           empty-text="Keine strategischen Auffälligkeiten." @highlight="emit('highlight', $event)" />
        <div v-if="analysis.material" class="q-mt-md text-body2">
          <strong>Materialveränderung:</strong> {{ analysis.material.before }} → {{ analysis.material.after }}
          (Bauerneinheiten aus deiner Sicht, am ruhigen Punkt der Hauptvariante)
        </div>
      </q-tab-panel>

      <q-tab-panel name="plans">
        <div class="text-subtitle2">Deine Pläne</div>
        <ul class="q-pl-md">
          <li v-for="(p, i) in userPlans" :key="i" class="text-body2">
            <strong>{{ p.title }}</strong> – {{ p.explanation }}
            <span class="sources">[{{ p.source === 'ENGINE' ? 'Engine' : 'Regel STRAT-011' }}: {{ p.basis }}]</span>
          </li>
          <li v-if="userPlans.length === 0" class="text-grey-7">Kein eindeutiger Plan aus den Stellungsmerkmalen.</li>
        </ul>
        <div class="text-subtitle2">Pläne des Gegners</div>
        <ul class="q-pl-md">
          <li v-for="(p, i) in opponentPlans" :key="i" class="text-body2">
            <strong>{{ p.title }}</strong> – {{ p.explanation }}
            <span class="sources">[{{ p.source === 'ENGINE' ? 'Engine' : 'Regel STRAT-011' }}: {{ p.basis }}]</span>
          </li>
          <li v-if="opponentPlans.length === 0" class="text-grey-7">Kein eindeutiger Plan aus den Stellungsmerkmalen.</li>
        </ul>
      </q-tab-panel>

      <q-tab-panel name="engine">
        <dl class="engine-facts text-body2">
          <dt>Bewertung vor dem Zug</dt><dd>{{ analysis.scoreBefore?.display }}</dd>
          <dt>Bewertung nach dem Zug</dt><dd>{{ analysis.scoreAfter?.display }}</dd>
          <dt>Bewertung des besten Zuges</dt><dd>{{ analysis.bestScore?.display }}</dd>
          <dt>Verlust gegenüber bestem Zug</dt>
          <dd>{{ analysis.evaluationLoss?.centipawns != null ? analysis.evaluationLoss.centipawns + ' Centipawns' : 'nicht als Centipawn-Wert bestimmbar (Matt)' }}
            <span v-if="analysis.evaluationLoss && !analysis.evaluationLoss.reliable"> – nicht zuverlässig</span></dd>
          <dt>Zuverlässigkeit</dt>
          <dd>{{ analysis.analysisConfidence }}<span v-if="analysis.confidenceReasons?.length"> ({{ analysis.confidenceReasons.join('; ') }})</span></dd>
          <dt>Einordnung</dt><dd>{{ (analysis.classificationReasons ?? []).join(' ') }}</dd>
          <dt>Engine</dt><dd>{{ analysis.engine?.name }} {{ analysis.engine?.version }}</dd>
          <dt>Suchgrenzen</dt><dd>{{ analysis.engine?.searchLimits }}</dd>
          <dt>Knoten / Zeit</dt><dd>{{ analysis.engine?.nodes }} / {{ analysis.engine?.elapsedMillis }} ms</dd>
          <dt>Regelversion</dt><dd>{{ analysis.rulesVersion }}</dd>
        </dl>
        <div class="row q-gutter-sm">
          <q-btn outline color="primary" icon="travel_explore" label="Varianten vertiefen" no-caps @click="emit('deepen')" />
          <q-btn v-if="analysis.outdated" outline color="orange-9" icon="refresh" label="Mit aktuellen Einstellungen neu analysieren"
                 no-caps @click="emit('reanalyse')" />
        </div>
      </q-tab-panel>
    </q-tab-panels>
  </q-card>
</template>

<style scoped>
.sources { color: #475569; font-size: 12px; margin-left: 4px; }
.engine-facts { display: grid; grid-template-columns: max-content 1fr; gap: 4px 12px; }
.engine-facts dt { font-weight: 600; }
.engine-facts dd { margin: 0; }
</style>
