(ns kixi.hecuba.profiles.app-model)

(def profile-schema {:timestamp {}
                     :profile_data {:event_type {}
                                    ;; Description
                                    :intervention_start_date {}
                                    :intervention_completion_date {}
                                    :intervention_description {}
                                    ;; Occupancy
                                    :occupancy_total {}
                                    :occupancy_under_18 {}
                                    :occupancy_18_to_60 {}
                                    :occupancy_over_60 {}
                                    :occupant_change {}
                                    ;; Measurement
                                    :footprint {}
                                    :external_perimeter {}
                                    :gross_internal_area {}
                                    :number_of_storeys {}
                                    :total_volume {}
                                    :total_rooms {}
                                    :bedroom_count {}
                                    :habitable_rooms {}
                                    :inadequate_heating {}
                                    :heated_habitable_rooms {}
                                    ;; Energy
                                    :ber {}
                                    :ter {}
                                    :primary_energy_requirement {}
                                    :space_heating_requirement {}
                                    :annual_space_heating_requirement {}
                                    :renewable_contribution_heat {}
                                    :renewable_contribution_elec {}
                                    :electricity_meter_type {}
                                    :mains_gas {}
                                    :electricity_storage_present {}
                                    :heat_storage_present {}
                                    ;; Efficiency
                                    :pipe_lagging {}
                                    :draught_proofing {}
                                    :draught_proofing_location {}
                                    ;; PassiveHaus
                                    :passive_solar_strategy {}
                                    :used_passivehaus_principles {}
                                    ;; Flats
                                    :flat_floors_in_block {}
                                    :flat_floor_position {}
                                    :flat_heat_loss_corridor {}
                                    :flat_heat_loss_corridor_other {}
                                    :flat_length_sheltered_wall {}
                                    :flat_floor_heat_loss_type {}
                                    ;; Fireplaces
                                    :open_fireplaces {}
                                    :sealed_fireplaces {}
                                    ;; Glazing
                                    :glazing_area_glass_only {}
                                    :glazing_area_percentage {}
                                    :multiple_glazing_type {}
                                    :multiple_glazing_area_percentage {}
                                    :multiple_glazing_u_value {}
                                    :multiple_glazing_type_other {}
                                    :frame_type {}
                                    :frame_type_other {}
                                    ;; Issues
                                    :moisture_condensation_mould_strategy {}
                                    :appliances_strategy {}
                                    :cellar_basement_issues {}
                                    ;; SAP Results
                                    :sap_rating {}
                                    :sap_performed_on {}
                                    :sap_assessor {}
                                    :sap_version_issue {}
                                    :sap_version_year {}
                                    :sap_regulations_date {}
                                    :sap_software {}
                                    ;; Lessons Learnt
                                    :thermal_bridging_strategy {}
                                    :airtightness_and_ventilation_strategy {}
                                    :overheating_cooling_strategy {}
                                    :controls_strategy {}
                                    :lighting_strategy {}
                                    :water_saving_strategy {}
                                    :innovation_approaches {}
                                    ;; Dwelling U Values Summary
                                    :best_u_value_for_doors {}
                                    :best_u_value_for_floors {}
                                    :best_u_value_for_other {}
                                    :best_u_value_for_roof {}
                                    :best_u_value_for_walls {}
                                    :best_u_value_for_windows {}
                                    :dwelling_u_value_other {}
                                    ;; Air Tightness Test
                                    :air_tightness_assessor {}
                                    :air_tightness_equipment {}
                                    :air_tightness_performed_on {}
                                    :air_tightness_rate {}
                                    ;; BUS Survey Information
                                    :profile_temperature_in_summer {}
                                    :profile_temperature_in_winter {}
                                    :profile_air_in_summer {}
                                    :profile_air_in_winter {}
                                    :profile_lightning {}
                                    :profile_noise {}
                                    :profile_comfort {}
                                    :profile_design {}
                                    :profile_needs {}
                                    :profile_health {}
                                    :profile_image_to_visitors {}
                                    :profile_productivity {}
                                    :profile_bus_summary_index {}
                                    :profile_bus_report_url {}
                                    ;; Project Details
                                    :total_budget_new_build {}
                                    :estimated_cost_new_build {}
                                    :final_cost_new_build {}
                                    :construction_time_new_build {}
                                    :design_guidance {}
                                    :planning_considerations {}
                                    :total_budget {}
                                    ;; Coheating Test
                                    :co_heating_loss {}
                                    :co_heating_performed_on {}
                                    :co_heating_assessor {}
                                    :co_heating_equipment {}}
                     :conservatories [{:conservatory_type {}
                                       :area {}
                                       :double_glazed {}
                                       :glazed_perimeter {}
                                       :height {}}]
                     :extensions [{:age {} :construction_date {}}]
                     :heating_systems [{:heating_type {}
                                        :heat_source {}
                                        :heat_transport {}
                                        :heat_delivery {}
                                        :heat_delivery_source {}
                                        :efficiency_derivation {}
                                        :boiler_type {}
                                        :boiler_type_other {}
                                        :fan_flue {}
                                        :open_flue {}
                                        :fuel {}
                                        :heating_system {}
                                        :heating_system_other {}
                                        :heating_system_type {}
                                        :heating_system_type_other {}
                                        :heating_system_solid_fuel {}
                                        :heating_system_solid_fuel_other {}
                                        :bed_index {}
                                        :make_and_model {}
                                        :controls {}
                                        :controls_other {}
                                        :controls_make_and_model {}
                                        :emitter {}
                                        :trvs_on_emitters {}
                                        :use_hours_per_week {}
                                        :installer {}
                                        :installer_engineers_name {}
                                        :installer_registration_number {}
                                        :commissioning_date {}
                                        :inspector {}
                                        :inspector_engineers_name {}
                                        :inspector_registration_number {}
                                        :inspection_date {}
                                        :efficiency {}}]
                     :hot_water_systems [{:dhw_type {}
                                          :fuel {}
                                          :fuel_oth {}
                                          :immersion {}
                                          :cylinder_capacity {}
                                          :cylinder_capacity_other {}
                                          :cylinder_insulation_type {}
                                          :cylinder_insulation_type_other {}
                                          :cylinder_insulation_thickness {}
                                          :cylinder_insulation_thickness_other {}
                                          :cylinder_thermostat {}
                                          :controls_same_for_all_zones {}}]
                     :storeys [{:storey_type {}
                                :storey {}
                                :heat_loss_w_per_k {}
                                :heat_requirement_kwth_per_year {}}]
                     :walls [{:wall_type {}
                              :construction {}
                              :construction_other {}
                              :insulation {}
                              :insulation_type {}
                              :insulation_thickness {}
                              :insulation_product {}
                              :uvalue {}
                              :location {}
                              :area {}}]
                     :roofs [{:roof_type {}
                              :construction {}
                              :construction_other {}
                              :insulation_location_one {}
                              :insulation_location_one_other {}
                              :insulation_location_two {}
                              :insulation_location_two_other {}
                              :insulation_thickness_one {}
                              :insulation_thickness_one_other {}
                              :insulation_thickness_two {}
                              :insulation_thickness_two_other {}
                              :insulation_date {}
                              :insulation_type {}
                              :insulation_product {}
                              :uvalue {}
                              :uvalue_derived {}}]
                     :window_sets [{:window_type {}
                                    :frame_type {}
                                    :frame_type_other {}
                                    :percentage_glazing {}
                                    :area {}
                                    :location {}
                                    :uvalue {}}]
                     :door_sets [{:door_type {}
                                  :door_type_other {}
                                  :frame_type {}
                                  :frame_type_other {}
                                  :percentage_glazing {}
                                  :area {}
                                  :location {}
                                  :uvalue {}}]
                     :floors [{:floor_type {}
                               :construction {}
                               :construction_other {}
                               :insulation_thickness_one {}
                               :insulation_thickness_two {}
                               :insulation_type {}
                               :insulation_product {}
                               :uvalue {}
                               :uvalue_derived {}}]
                     :roof_rooms [{:location {}
                                   :age {}
                                   :insulation_placement {}
                                   :insulation_thickness_one {}
                                   :insulation_thickness_one_other {}
                                   :insulation_thickness_two {}
                                   :insulation_thickness_two_other {}
                                   :insulation_date {}
                                   :insulation_type {}
                                   :insulation_product {}
                                   :uvalue {}
                                   :uvalue_derived {}}]
                     :low_energy_lights [{:light_type {}
                                          :light_type_other {}
                                          :bed_index {}
                                          :proportion {}}]
                     :ventilation_systems [{:approach {}
                                            :approach_other {}
                                            :ventilation_type {}
                                            :ventilation_type_other {}
                                            :mechanical_with_heat_recovery {}
                                            :manufacturer {}
                                            :ductwork_type {}
                                            :ductwork_type_other {}
                                            :controls {}
                                            :controls_other {}
                                            :manual_control_location {}
                                            :operational_settings {}
                                            :operational_settings_other {}
                                            :installer {}
                                            :installer_engineers_name {}
                                            :installer_registration_number {}
                                            :commissioning_date {}
                                            :total_installed_area {}}]
                     :airflow_measurements [{:reference {}
                                             :system {}
                                             :inspector {}
                                             :inspector_engineers_name {}
                                             :inspector_registration_number {}
                                             :inspection_date {}
                                             :measured_low {}
                                             :design_low {}
                                             :measured_high {}
                                             :design_high {}}]
                     :photovoltaics [{:percentage_roof_covered {}
                                      :photovoltaic_type {}
                                      :photovoltaic_type_other {}
                                      :make_model {}
                                      :mcs_no {}
                                      :installer {}
                                      :installer_mcs_no {}
                                      :commissioning_date {}
                                      :capacity {}
                                      :area {}
                                      :orientation {}
                                      :pitch {}
                                      :est_annual_generation {}
                                      :est_percentage_requirement_met {}}]
                     :solar_thermals [{:solar_type {}
                                       :solar_type_other {}
                                       :make_model {}
                                       :mcs_no {}
                                       :installer {}
                                       :installer_mcs_no {}
                                       :commissioning_date {}
                                       :capacity {}
                                       :area {}
                                       :orientation {}
                                       :pitch {}
                                       :est_annual_generation {}
                                       :est_percentage_requirement_met {}}]
                     :wind_turbines [{:turbine_type {}
                                      :turbine_type_other {}
                                      :make_model {}
                                      :mcs_no {}
                                      :inverter_type {}
                                      :inverter_make_model {}
                                      :inverter_mcs_no {}
                                      :installer {}
                                      :installer_mcs_no {}
                                      :commissioning_date {}
                                      :capacity {}
                                      :hub_height {}
                                      :height_above_canopy {}
                                      :wind_speed {}
                                      :wind_speed_info_source {}
                                      :est_annual_generation {}
                                      :est_percentage_requirement_met {}
                                      :est_percentage_exported {}}]
                     :heat_pumps [{:heat_pump_type {}
                                   :make_model {}
                                   :cop {}
                                   :spf {}
                                   :mcs_no {}
                                   :installer {}
                                   :installer_mcs_no {}
                                   :commissioning_date {}
                                   :heat_source_type {}
                                   :heat_source_type_other {}
                                   :depth {}
                                   :geology {}
                                   :capacity {}
                                   :est_annual_generation {}
                                   :est_percentage_requirement_met {}
                                   :dhw {}
                                   :est_percentage_dhw_requirement_met {}}]
                     :biomasses [{:biomass_type {}
                                  :model {}
                                  :mcs_no {}
                                  :installer {}
                                  :installer_mcs_no {}
                                  :commissioning_date {}
                                  :capacity {}
                                  :percentage_efficiency_from_spec {}
                                  :est_annual_generation {}
                                  :est_percentage_requirement_met {}}]
                     :chps [{:chp_type {}
                             :model {}
                             :mcs_no {}
                             :installer {}
                             :installer_mcs_no {}
                             :commissioning_date {}
                             :capacity_elec {}
                             :capacity_thermal {}
                             :est_annual_generation {}
                             :est_percentage_thermal_requirement_met {}
                             :est_percentage_exported {}}]})

(def app-model (atom
                {:new-profile (assoc profile-schema :alert {})}))
