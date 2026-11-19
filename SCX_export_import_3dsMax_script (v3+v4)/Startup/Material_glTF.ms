-- A scripted implementation of a glTF PBR Next material frontend
-- Copyright 2021 Autodesk Inc, All rights reserved. This file is licensed under Apache 2.0 license
-- You are free to modify this file to make derivative versions to match your workflow/engine.
-- Remember to change the ClassID if you do...
--    https://github.com/ADN-DevTech/3dsMax-OSL-Shaders/blob/master/LICENSE.txt

plugin material glTFMaterial
name:~PBR_MATERIAL_glTFPBRN~
nonLocalizedName:"glTF Material"
classID:#(0x38420192, 0x45fe4e1b)
extends:PhysicalMaterial replaceUI:true version:1
(
	local params_pbrnext
	local params_gltfext
	local opacity_output_map = undefined
	local oslColorFloatMult = "shader mul (color a = 1, float b = 1, output color out = 1) { out = a * b; }"
	local oslColorMult = "shader mul (color a = 1, color b = 1, output color out = 1) { out = a * b; }"
	local oslColorMix = "shader mixCol (color a = 1, color b = 1, float m = 1, output color out = 0) { out = mix(a,b,m); }"
	local oslCutout = "shader gltfcutout (color opacitymap = 0, float opacityfactor = 0.0, output color out = 1) { out = ((opacitymap[0]+opacitymap[1]+opacitymap[2])/3.0)>=opacityfactor?1.0:0.0; }"
	local updateMapOnFactorChange = true

	-- Layout constants
	local mapBtnWidth = 160
	local spinnerWidth = 40
	local colPickerWidth = 50
	local heightIndent = 2 -- general y-offset to align labels, spinners better to the vcenter of the map button
	local paramLabelIndentLvl2 = 25 -- label indent on the second ui level
	local paramLabelIndentLvl3 = paramLabelIndentLvl2+10 -- lab el indent on the third ui level
	local spinnerIndent3Cols = -5 -- base control indent for across:3, column in the middle
	local colPickerIndent3Cols = spinnerIndent3Cols-2 -- indent for across:3
	local spinnerIndent2Cols = spinnerIndent3Cols-54 -- indent for across:2
	local colPickerIndent2Cols = spinnerIndent2Cols-2 -- indent for across:2
	local checkboxIndentLvl1 = -2 -- checkbox indent on the first ui level
	local checkboxIndentLvl2 = 13 -- checkbox indent on the second ui level
	local minCtrlHeight = 25 -- set on e.g. checkboxes on rows without map button, so that the row height is not to small

	fn createOSLMap oslCode =
	(
		local osl_map = OSLMap()
		-- Turn off undo system for shader assignment since this deletes and rebuilts the OSLMaps's paramblock, resulting in a loss of undo history.
		with undo off 
		(
			osl_map.oslCode = oslCode
		)
		return osl_map
	)
	
	fn updateCutoutMap mode maskmap factor =
	(
		if (not theHold.RestoreOrRedoing()) do
		(
			-- Opaque
			if (mode == 1) then
			(
				-- In this case the Cutout Map is not used
				delegate.cutout_map = undefined
				opacity_output_map = undefined
			)
			-- Mask
			else if (mode == 2) then 
			(
				if (opacity_output_map == undefined) then
				(
					opacity_output_map = createOSLMap oslCutout
				)

				opacity_output_map.opacitymap_map = maskmap
				opacity_output_map.opacityfactor = factor
				
				-- set Cutout Map of the Physical Material
				delegate.cutout_map = opacity_output_map
			)
			-- Blend
			else if (mode == 3) then 
			(
				-- Use the Cutout Map directly
				delegate.cutout_map = maskmap
				opacity_output_map = undefined
			)
		)
	)
	
	fn FloatsEqual val1 val2 =
	(
		val1 == val2 OR abs(val1 - val2) <= 0.000001
	)

	-- Sets the map factor parameter to the specified value if the parameter value equals the expected value e.g. 0.
	-- This function is called for multiplied maps on map change.
	fn autoAssignFactorOnMapChange map &param expVal autoVal =
	(
		if (loading == false and map != undefined and (FloatsEqual param expVal)) then
		(
			updateMapOnFactorChange  = false
			param = autoVal
			updateMapOnFactorChange  = true
		)
	)
	
	-- Sets the map factor parameter to the specified value if the parameter value equals the expected value e.g. Black.
	-- This function is called for multiplied maps on map change.
	fn autoAssignColorFactorOnMapChange map &param expVal autoVal =
	(
		if (loading == false and map != undefined and param == expVal ) then
		(
			updateMapOnFactorChange  = false
			param = autoVal
			updateMapOnFactorChange  = true
		)
	)

	fn doFloatMultiplyMapOnDelegate &delegate_map map mapFactor = 
	(
		if (updateMapOnFactorChange and not theHold.RestoreOrRedoing()) do
		(
			if (map != undefined and mapFactor != 1.0 ) then
			(
				local mul = createOSLMap oslColorFloatMult
				mul.a_map = map
				mul.b = mapFactor
				delegate_map = mul
			)
			else 
			(
				delegate_map = map
			)
		)
	)

	fn doColorMultiplyMapOnDelegate &delegate_map map mapColorFactor = 
	(
		if (updateMapOnFactorChange and not theHold.RestoreOrRedoing()) do
		(
			if (map != undefined and mapColorFactor != white) then
			(
				local mul = createOSLMap oslColorMult
				mul.a = mapColorFactor
				mul.b_map = map
				delegate_map = mul
			)
			else
			(
				delegate_map = map
			)
		)
	)

	-- Used for blending e.g. the occlusion map on the materials base color.
	fn doColorMixMapOnDelegate &delegate_map map mixValue = 
	(
		if (updateMapOnFactorChange and not theHold.RestoreOrRedoing()) do
		(
			if (map != undefined and mixValue != 1.0 ) then
			(
				local mixCol = createOSLMap oslColorMix
				mixCol.a = white
				mixCol.b_map = map
				mixCol.m = mixValue
				delegate_map = mixCol
			)
			else 
			(
				delegate_map = map
			)
		)
	)

	-- Helper function for enabling/disabling the IOR effect on the delegate and gltf material.
	fn enableIORParams enable =
	(
		if enable then
		(
			this.indexOfRefraction = this.indexOfRefraction
		)
		else
		(
			delegate.trans_ior = 1.52
		)
	)

	-- Helper function for enabling/disabling the volume effect on the delegate and gltf material.
	fn enableVol enable =
	(
		if enable then
		(
			-- When Volume is reenabled, we need to trigger an update to the Physical Material although the values of the glTF Material havent changed.
			this.volumeThickness = this.volumeThickness
			this.volumeDistance = this.volumeDistance
			this.volumeColor = this.volumeColor
		)
		else
		(
			-- If Volume is disabled, we need to remove its effect on the underlying Physical Material
			-- The UI stays the same but is disabled.
			delegate.thin_walled = true
			delegate.trans_depth = 0.0
			-- If transmission is still on, the base color will dictate the transparency color of the delegate
			if ( this.enableTransmission ) then
			(
				delegate.trans_color = delegate.base_color
				delegate.trans_color_map = delegate.base_color_map
			)
			else
			(
				delegate.trans_color = white
				delegate.trans_color_map = undefined
			)
		)
	)

	-- Helper function for enabling/disabling the metalness effect on the delegate and gltf material.
	fn enableMetalnessParams enable =
	(
		if enable then
		(
			this.metalness = this.metalness
			this.metalnessMap = this.metalnessMap
		)
		else
		(
			delegate.metalness = 0
			delegate.metalness_map = undefined
		)
	)

	-- Helper function for enabling/disabling the roughness effect on the delegate and gltf material.
	fn enableRoughnessParams enable =
	(
		if enable then
		(
			this.roughness = this.roughness
			this.roughnessMap = this.roughnessMap
		)
		else
		(
			delegate.roughness = 1
			delegate.Roughness_Map = undefined
		)
	)

	-- Helper function for enabling/disabling the normal effect on the delegate and gltf material.
	fn enableNormalParams enable =
	(
		if enable then
		(
			this.normal = this.normal
			this.normalMap = this.normalMap
		)
		else
		(
			delegate.bump_map_amt = 0
			delegate.bump_map = undefined
		)
	)

	-- Helper function for enabling/disabling the ambient occlusion effect on the delegate and gltf material.
	fn enableOcclusionParams enable =
	(
		if enable then
		(
			this.ambientOcclusionMap = this.ambientOcclusionMap
		)
		else
		(
			delegate.base_weight_map = undefined
		)
	)

	-- Helper function for enabling/disabling the emission effect on the delegate and gltf material.
	fn enableEmissionParams enable =
	(
		if enable then
		(
			delegate.emission = 1.0
			this.emissionColor = this.emissionColor
			this.emissionMap = this.emissionMap
		)
		else
		(
			delegate.emission = 0.0
			delegate.emit_color = black
			delegate.emit_color_map = undefined
		)
	)

	-- Helper function for enabling/disabling the clearcoat effect on the delegate and gltf material.
	fn enableClearCoatParams enable =
	(
		if enable then
		(
			-- When Clearcoat is reenabled, we need to trigger an update to the Physical Material although the values of the glTF Material havent changed.
			this.clearcoat = this.clearcoat
			this.clearcoatMap = this.clearcoatMap
			this.clearcoatRoughness = this.clearcoatRoughness
			this.clearcoatRoughnessMap = this.clearcoatRoughnessMap
			this.clearcoatNormalMap = this.clearcoatNormalMap
		)
		else
		(
			-- If Clearcoat is disabled, we need to remove its effect on the underlying Physical Material
			-- The UI stays the same but is disabled.
			delegate.coating = 0.0
			delegate.coat_map = undefined
			delegate.coat_roughness = 0.0
			delegate.coat_rough_map = undefined
			delegate.coat_bump_map = undefined
		)
	)

	-- Helper function for enabling/disabling the sheen effect on the delegate and gltf material.
	fn enableSheenParams enable =
	(
		-- (Sheen_Weight)
		delegate.sheen = if enable then 1.0 else 0.0
	)

	-- Helper function for enabling/disabling the specular effect on the delegate and gltf material.
	fn enableSpecularParams enable =
	(
		if enable then
		(
			delegate.reflectivity = this.specular
			this.specularMap = this.specularMap
			delegate.refl_color = this.specularColor
			this.specularColorMap = this.specularColorMap
		)
		else
		(
			delegate.reflectivity = if this.unlit then 0.0 else 1.0
			delegate.reflectivity_map = undefined
			delegate.refl_color = if this.unlit then black else white
			delegate.refl_color_map = undefined
		)
	)

	-- Helper function for enabling/disabling the transmission effect on the delegate and gltf material.
	fn enableTransmissionParams enable =
	(
		if enable then
		(
			delegate.transparency = this.transmission
			this.transmissionMap = this.transmissionMap
			if ( not this.enableVolume ) do
			(
				delegate.trans_color_map = delegate.base_color_map
				delegate.trans_color = delegate.base_color
			)
		)
		else
		(
			delegate.transparency = 0.0
			delegate.transparency_map = undefined
			delegate.trans_color_map = undefined
			delegate.trans_color = white
		)
		-- Enable/disable dependent sub parameters
		enableVol (enable and this.enableVolume)
	)

	-- Helper function to apply the base color on the emission when unlit material is on.
	-- This will allow a better preview in the viewport.
	fn applyBaseColorOnEmissionDelegate =
	(
		delegate.emit_color = this.baseColor
		delegate.emission = 1.0
		delegate.luminance = 477.7 -- default emission luminance too high compared to GLTF viewer
		doColorMultiplyMapOnDelegate &delegate.emit_color_map this.baseColorMap this.baseColor
	)

	-- Helper function for enabling/disabling the unlit material effect on the delegate and gltf material.
	fn enableUnlitMaterialParams enable =
	(
		local enableExt = (not enable)
		
		-- Standard gltf param rollout
		-- disable/enable parameters on delegate level
		enableMetalnessParams enableExt
		enableRoughnessParams enableExt
		enableNormalParams enableExt
		enableOcclusionParams enableExt
		
		if enableExt then
		(
			this.baseColor = this.baseColor
			this.baseColorMap = this.baseColorMap
			enableEmissionParams enableExt
		)
		else
		(
			delegate.base_color = black
			delegate.base_color_map = undefined
			applyBaseColorOnEmissionDelegate()
		)
		
		-- disable/enable UI
		params_pbrnext.UpdateMetalnessGroupUI enableExt
		params_pbrnext.UpdateRoughnessGroupUI enableExt
		params_pbrnext.UpdateNormalGroupUI enableExt
		params_pbrnext.UpdateOcclusionGroupUI enableExt
		params_pbrnext.UpdateEmissionGroupUI enableExt
		
		-- Extension gltf param rollout
		-- disable/enable parameters on delegate level
		enableClearCoatParams (enableExt and this.enableClearCoat)
		enableSheenParams (enableExt and this.enableSheen)
		enableSpecularParams (enableExt and this.enableSpecular)
		enableTransmissionParams (enableExt and this.enableTransmission)
		enableIORParams (enableExt and this.enableIndexOfRefraction)
		-- disable/enable UI
		params_gltfext.cbEnableClearcoat.enabled = enableExt
		params_gltfext.UpdateClearcoatGroupUI (enableExt and this.enableClearCoat)
		params_gltfext.cbEnableSheen.enabled = enableExt
		params_gltfext.UpdateSheenGroupUI (enableExt and this.enableSheen)
		params_gltfext.cbEnableSpecular.enabled = enableExt
		params_gltfext.UpdateSpecularGroupUI (enableExt and this.enableSpecular)
		params_gltfext.cbEnableTransmission.enabled = enableExt
		params_gltfext.UpdateTransmissionGroupUI (enableExt and this.enableTransmission)
		params_gltfext.cbEnableIOR.enabled = enableExt
		params_gltfext.UpdateIORGroupUI (enableExt and this.enableIndexOfRefraction)
	)

	-- This method set the gamma to 1 on texture maps that are not meant to be RGB.
	fn setGamma1 map = 
	(
		if (classof map == bitmapTex) do
		(
			-- Accessing .bitmap of a missing map yeilds an exception
			try
			(
				if (map.bitmap != undefined) do 
				(
					if (map.bitmap.gamma != 1.0) do 
					(
						local gamma1bmp = openBitmap map.bitmap.filename gamma:1.0
						if (gamma1bmp != undefined) do map.bitmap = gamma1bmp
					)
				)
			)
			catch() -- do-nothing catch
		)
		if (classOf map == OSLMap) do 
		(
			if (hasProperty map "autoGamma") do
			(
				map.autogamma = false
				
				if (hasProperty map "manualGamma") do 
					map.manualGamma = 1.0
			)
		)
		if (classOf map == MultiOutputChannelTexmapToTexmap) do
		(
			setGamma1(map.sourceMap)
		)
	)

	parameters main rollout:params_pbrnext 
	(
		baseColor type:#frgba default:white ui:cpBaseColor localizedname:~GLTF_MATERIAL_PARAM_BASECOLOR~ nonlocalizedName:"Base Color"
		baseColorMap type:#TextureMap ui:btnBaseColorMap localizedname:~GLTF_MATERIAL_PARAM_BASECOLOR_MAP~ nonlocalizedName:"Base Color Map"
		
		alphaMode type:#integer default:1 ui:cbAlphaMode localizedname:~GLTF_MATERIAL_PARAM_ALPHA_MODE~ nonlocalizedName:"Alpha"
		alphaMap type:#textureMap ui:btnAlphaMap localizedname:~GLTF_MATERIAL_PARAM_ALPHA_MAP~ nonlocalizedName:"Alpha Map"
		alphaCutoff type:#float default:0.5 ui:spnAlphaCutoff localizedname:~GLTF_MATERIAL_PARAM_ALPHA_CUTOFF~ nonlocalizedName:"Cutoff"
		
		metalness type:#float default:0.0 ui:spnMetalness localizedname:~GLTF_MATERIAL_PARAM_METALNESS~
		metalnessMap type:#textureMap ui:btnMetalnessMap localizedname:~GLTF_MATERIAL_PARAM_METALNESS_MAP~ nonlocalizedName:"Metalness Map"
		
		roughness type:#float default:0.0 ui:spnRoughness localizedname:~GLTF_MATERIAL_PARAM_ROUGHNESS~
		roughnessMap type:#textureMap ui:btnRoughnessMap localizedname:~GLTF_MATERIAL_PARAM_ROUGHNESS_MAP~ nonlocalizedName:"Roughness Map"
		
		normal type:#float default:1.0 ui:spnNormal localizedname:~GLTF_MATERIAL_PARAM_NORMAL~
		normalMap type:#textureMap ui:btnNormalMap localizedname:~GLTF_MATERIAL_PARAM_NORMAL_MAP~ nonlocalizedName:"Normal Map"

		ambientOcclusion type:#float default:1.0 ui:spnAmbientOcclusion localizedname:~GLTF_MATERIAL_PARAM_AO~ nonlocalizedName:"Occlusion (AO)"
		ambientOcclusionMap type:#textureMap ui:btnAmbientOcclusionMap localizedname:~GLTF_MATERIAL_PARAM_AO_MAP~ nonlocalizedName:"Occlusion (AO) Map"
		
		emissionColor type:#frgba default:black ui:cpEmissionColor localizedname:~GLTF_MATERIAL_PARAM_EMISSION~ nonlocalizedName:"Emission"
		emissionMap type:#textureMap ui:btnEmissionMap localizedname:~GLTF_MATERIAL_PARAM_EMISSION_MAP~ nonlocalizedName:"Emission Map"
		
		doubleSided type:#boolean default:false ui:cbDoubleSided localizedname:~GLTF_MATERIAL_PARAM_DOUBLE_SIDED~ nonlocalizedName:"Double Sided"

		on basecolor set val do
		(
			delegate.base_weight = 1.0
			if (this.unlit) then 
			(
				delegate.base_color = black
				applyBaseColorOnEmissionDelegate()
			)
			else 
			(
				delegate.base_color = val
				doColorMultiplyMapOnDelegate &delegate.base_color_map baseColorMap val
	
				if(this.enableTransmission and not this.enableVolume) do
				(
					delegate.trans_color = delegate.base_color
					delegate.trans_color_map = delegate.base_color_map
				)
			)
		)

		on baseColorMap set val do
		(
			delegate.base_weight = 1.0
			-- Set base color factor to white if it is black on map assignment
			autoAssignColorFactorOnMapChange val &basecolor black white

			if (this.unlit) then
			(
				delegate.base_color_map = undefined
				applyBaseColorOnEmissionDelegate()
			)
			else
			(
				doColorMultiplyMapOnDelegate &delegate.base_color_map val basecolor

				if(this.enableTransmission and not this.enableVolume) do
				(
					delegate.trans_color_map = delegate.base_color_map
				)
			)
		)
		
		on alphaMode set val do
		(
			updateCutoutMap val alphaMap alphaCutoff
			params_pbrnext.UpdateBaseColorGroupUI alphaMode
		)

		on roughness set val do 
		(
			delegate.roughness = val
			doFloatMultiplyMapOnDelegate &delegate.Roughness_Map roughnessMap val
		)

		on roughnessMap set val do 
		(
			-- Set roughness factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &roughness 0.0 1.0
			doFloatMultiplyMapOnDelegate &delegate.Roughness_Map val roughness
		)
		
		on alphaMap set val do
		(
			updateCutoutMap alphaMode val alphaCutoff
		)
		
		on alphaCutoff set val do
		(
			updateCutoutMap alphaMode alphaMap val
		)

		on metalness set val do
		(
			if (this.unlit == false) then
			(
				delegate.metalness = val
				doFloatMultiplyMapOnDelegate &delegate.metalness_map metalnessMap val
			)
		)

		on metalnessMap set val do
		(
			-- Set metalness factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &metalness 0.0 1.0
			if (this.unlit == false) then
			(
				doFloatMultiplyMapOnDelegate &delegate.metalness_map val metalness
			)
		)

		on normal set val do
		(
			if (this.unlit == false) then
			(
				delegate.bump_map_amt = val
			)
		)

		on normalMap set val do 
		(
			setGamma1 val
			-- Set normal bump factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &normal 0.0 1.0
			if (this.unlit == false) then
			(
				if (val != undefined) then 
				(
					local bm = Gnormal()
					bm.normal_map = val
					bm.flipgreen = true -- glTF expects the normal map's green channel to be 'up', contrary to directX
					delegate.bump_map = bm
				)
				else
				(
					delegate.bump_map = undefined
				)
			)
		)

		on ambientOcclusion set val do
		(
			if (this.unlit == false) then
			(
				doColorMixMapOnDelegate &delegate.base_weight_map ambientOcclusionMap val
			)
		)
		
		on ambientOcclusionMap set val do
		(
			-- Set occlusion factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &ambientOcclusion 0.0 1.0
			if (this.unlit == false) then
			(
				doColorMixMapOnDelegate &delegate.base_weight_map val ambientOcclusion
			)
		)

		on emissionColor set val do
		(
			if (this.unlit == false) then
			(
				delegate.emit_color = val
				delegate.emission = 1.0
				delegate.luminance = 477.7 -- default emission luminance too high compared to GLTF viewer
				doColorMultiplyMapOnDelegate &delegate.emit_color_map emissionMap val
			)
		)

		on emissionMap set val do
		(
			-- Set emission color factor to white if it is black on map assignment
			autoAssignColorFactorOnMapChange val &emissionColor black white
			if (this.unlit == false) then
			(
				delegate.emission = 1.0
				delegate.luminance = 477.7 -- default emission luminance too high compared to GLTF viewer
				doColorMultiplyMapOnDelegate &delegate.emit_color_map val emissionColor
			)
		)
	)

	parameters extension rollout:params_gltfext
	(
		unlit type:#boolean default:false ui:cbUnlitMaterials localizedname:~GLTF_MATERIAL_PARAM_UNLIT~

		enableClearcoat type:#boolean default:false ui:cbEnableClearcoat localizedname:~GLTF_MATERIAL_PARAM_ENABLE_CLEARCOAT~ nonlocalizedName:"Enable Clearcoat"
		clearcoat type:#float default:1.0 ui:spnClearcoatFactor localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT~
		clearcoatMap type:#TextureMap ui:btnClearcoatMap localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT_MAP~ nonlocalizedName:"Clearcoat Map"

		clearcoatRoughness type:#float default:0.0 ui:spnClearcoatRoughness localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT_ROUGHNESS~ nonlocalizedName:"Clearcoat Roughness"
		clearcoatRoughnessMap type:#TextureMap ui:btnClearcoatRoughnessMap localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT_ROUGHNESS_MAP~ nonlocalizedName:"Clearcoat Roughness Map"
		clearcoatNormal type:#float default:1.0 ui:spnClearcoatNormal localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT_NORMAL~ nonlocalizedName:"Clearcoat Normal"
		clearcoatNormalMap type:#TextureMap ui:btnClearcoatNormalMap localizedname:~GLTF_MATERIAL_PARAM_CLEARCOAT_NORMAL_MAP~ nonlocalizedName:"Clearcoat Normal Map"

		enableSheen type:#boolean default:false ui:cbEnableSheen localizedname:~GLTF_MATERIAL_PARAM_ENABLE_SHEEN~ nonlocalizedName:"Enable Sheen"
		sheenColor type:#frgba default:white ui:cpSheenColor localizedname:~GLTF_MATERIAL_PARAM_SHEEN_COLOR~ nonlocalizedName:"Sheen Color"
		sheenColorMap type:#TextureMap ui:btnSheenColorMap localizedname:~GLTF_MATERIAL_PARAM_SHEEN_COLOR_MAP~ nonlocalizedName:"Sheen Color Map"
		sheenRoughness type:#float default:0.0 ui:spnSheenRoughness localizedname:~GLTF_MATERIAL_PARAM_SHEEN_ROUGHNESS~ nonlocalizedName:"Sheen Roughness"
		sheenRoughnessMap type:#TextureMap ui:btnSheenRoughnessMap localizedname:~GLTF_MATERIAL_PARAM_SHEEN_ROUGHNESS_MAP~ nonlocalizedName:"Sheen Roughness Map"

		enableSpecular type:#boolean default:false ui:cbEnableSpecular localizedname:~GLTF_MATERIAL_PARAM_ENABLE_SPECULAR~ nonlocalizedName:"Enable Specular"
		specular type:#float default:1.0 ui:spnSpecularFactor localizedname:~GLTF_MATERIAL_PARAM_SPECULAR~
		specularMap type:#TextureMap ui:btnSpecularMap localizedname:~GLTF_MATERIAL_PARAM_SPECULAR_MAP~ nonlocalizedName:"Specular Map"
		specularColor type:#frgba default:white ui:cpSpecularColor localizedname:~GLTF_MATERIAL_PARAM_SPECULAR_COLOR~ nonlocalizedName:"Specular Color"
		specularColorMap type:#TextureMap ui:btnSpecularColorMap localizedname:~GLTF_MATERIAL_PARAM_SPECULAR_COLOR_MAP~ nonlocalizedName:"Specular Color Map"

		enableTransmission type:#boolean default:false ui:cbEnableTransmission localizedname:~GLTF_MATERIAL_PARAM_ENABLE_TRANSMISSION~ nonlocalizedName:"Enable Transmission"
		transmission type:#float default:1.0 ui:spnTransmissionFactor localizedname:~GLTF_MATERIAL_PARAM_TRANSMISSION~
		transmissionMap type:#TextureMap ui:btnTransmissionMap localizedname:~GLTF_MATERIAL_PARAM_TRANSMISSION_MAP~ nonlocalizedName:"Transmission Map"

		enableVolume type:#boolean default:false ui:cbEnableVolume localizedname:~GLTF_MATERIAL_PARAM_ENABLE_VOLUME~ nonlocalizedName:"Enable Volume"
		volumeThickness type:#float default:0.0 ui:spnVolumeThickness localizedname:~GLTF_MATERIAL_PARAM_VOLUME_THICKNESS~ nonlocalizedName:"Volume Thickness"
		volumeThicknessMap type:#TextureMap ui:btnVolumeThicknessMap localizedname:~GLTF_MATERIAL_PARAM_VOLUME_THICKNESS_MAP~ nonlocalizedName:"Volume Thickness Map"
		volumeDistance type:#float default:0.0 ui:spnVolumeDistance localizedname:~GLTF_MATERIAL_PARAM_VOLUME_DISTANCE~ nonlocalizedName:"Volume Distance"
		volumeColor type:#frgba default:white ui:cpVolumeColor localizedname:~GLTF_MATERIAL_PARAM_VOLUME_COLOR~ nonlocalizedName:"Volume Color"

		enableIndexOfRefraction type:#boolean default:false ui:cbEnableIOR localizedname:~GLTF_MATERIAL_PARAM_ENABLE_IOR~ nonlocalizedName:"Enable IOR"
		indexOfRefraction type:#float default:1.5 ui:spnIOR localizedname:~GLTF_MATERIAL_PARAM_IOR~ nonlocalizedName:"IOR"

		on enableClearcoat set val do
		(
			enableClearCoatParams val
			params_gltfext.UpdateClearcoatGroupUI enableClearcoat
		)

		on clearcoat set val do
		(
			if (enableClearcoat and not unlit) then
			(
				delegate.coating = val
				doFloatMultiplyMapOnDelegate &delegate.coat_map clearcoatMap val
			)
		)

		on clearcoatMap set val do
		(
			-- Set clearcoat factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &clearcoat 0.0 1.0
			if (enableClearcoat and not unlit) then
			(
				doFloatMultiplyMapOnDelegate &delegate.coat_map val clearcoat
			)
		)

		on clearcoatRoughness set val do
		(
			if (enableClearcoat and not unlit) then
			(
				delegate.coat_roughness = val
				doFloatMultiplyMapOnDelegate &delegate.coat_rough_map clearcoatRoughnessMap val
			)
		)

		on clearcoatRoughnessMap set val do
		(
			-- Set roughness factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &clearcoatRoughness 0.0 1.0
			if (enableClearcoat and not unlit) then
			(
				doFloatMultiplyMapOnDelegate &delegate.coat_rough_map val clearcoatRoughness
			)
		)

		on clearcoatNormal set val do
		(
			delegate.clearcoat_bump_map_amt = val
		)

		on clearcoatNormalMap set val do 
		(
			setGamma1 val
			-- Set normal bump factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &clearcoatNormal 0.0 1.0
			if (enableClearcoat and not unlit) then
			(
				if (val != undefined) then 
				(
					local bm = Gnormal()
					bm.normal_map = val
					bm.flipgreen = true -- glTF expects the normal map's green channel to be 'up', contrary to directX
					delegate.coat_bump_map = bm
				)
				else
				(
					delegate.coat_bump_map = undefined
				)
			)
		)

		on enableSheen set val do
		(
			enableSheenParams val
			params_gltfext.UpdateSheenGroupUI enableSheen
		)

		on sheenColor set val do
		(
			if (enableSheen and not unlit) then
			(
				delegate.sheen_color = val
				doColorMultiplyMapOnDelegate &delegate.sheen_color_map sheenColorMap val
			)
		)

		on sheenColorMap set val do
		(
			-- Set sheen color factor to white if it is black on map assignment
			autoAssignColorFactorOnMapChange val &sheenColor black white
			if (enableSheen and not unlit) then
			(
				doColorMultiplyMapOnDelegate &delegate.sheen_color_map val sheenColor
			)
		)

		on sheenRoughness set val do
		(
			if (enableSheen and not unlit) then
			(
				delegate.sheen_roughness = val
				doFloatMultiplyMapOnDelegate &delegate.sheen_rough_map sheenRoughnessMap val
			)
		)

		on sheenRoughnessMap set val do
		(
			-- Set roughness factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &sheenRoughness 0.0 1.0
			if (enableSheen and not unlit) then
			(
				doFloatMultiplyMapOnDelegate &delegate.sheen_rough_map val sheenRoughness
			)
		)

		on enableIndexOfRefraction set val do
		(
			enableIORParams val
			params_gltfext.UpdateIORGroupUI enableIndexOfRefraction
		)

		on indexOfRefraction set val do
		(
			if (enableIndexOfRefraction and not unlit) then
			(
				delegate.trans_ior = val
			)
		)

		on enableVolume set val do
		(
			enableVol val
			params_gltfext.UpdateVolumeGroupUI enableVolume
		)

		on volumeThickness set val do
		(
			if (enableVolume and not unlit) then
			(
				isZero = FloatsEqual val 0
				delegate.thin_walled = isZero
				delegate.trans_depth = volumeDistance / val
				if ( isZero ) then
				(
					delegate.trans_color = white
				)
				else
				(
					delegate.trans_color = volumeColor
				)
			)
		)

		on volumeThicknessMap set val do
		(
			-- Set the thickness factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &volumeThickness 0.0 1.0
		)

		on volumeDistance set val do
		(
			if (enableVolume and not unlit) then
			(
				delegate.trans_depth = val / volumeThickness
			)
		)

		on volumeColor set val do
		(
			if (enableVolume and not unlit and not (FloatsEqual volumeThickness 0)) then
			(
				delegate.trans_color = val
			)
		)

		on enableTransmission set val do
		(
			enableTransmissionParams val
			params_gltfext.UpdateTransmissionGroupUI enableTransmission
		)

		on transmission set val do
		(
			if (enableTransmission and not unlit) then
			(
				delegate.transparency = val
				doFloatMultiplyMapOnDelegate &delegate.transparency_map transmissionMap val
			)
		)

		on transmissionMap set val do
		(
			-- Set transmission factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &transmission 0.0 1.0
			if (enableTransmission and not unlit) then
			(
				doFloatMultiplyMapOnDelegate &delegate.transparency_map val transmission
			)
		)

		on enableSpecular set val do
		(
			enableSpecularParams val
			params_gltfext.UpdateSpecularGroupUI enableSpecular
		)

		on specular set val do
		(
			if (enableSpecular and not unlit) then
			(
				delegate.reflectivity = val
				doFloatMultiplyMapOnDelegate &delegate.reflectivity_map specularMap val
			)
		)

		on specularMap set val do
		(
			-- Set specular factor to 1 if it is 0 on map assignment
			autoAssignFactorOnMapChange val &specular 0.0 1.0
			if (enableSpecular and not unlit) then
			(
				doFloatMultiplyMapOnDelegate &delegate.reflectivity_map val specular
			)
		)

		on specularColor set val do
		(
			if (enableSpecular and not unlit) then
			(
				delegate.refl_color = val
				doColorMultiplyMapOnDelegate &delegate.refl_color_map specularColorMap val
			)
		)

		on specularColorMap set val do
		(
			-- Set specular color factor to white if it is black on map assignment
			autoAssignColorFactorOnMapChange val &specularColor black white
			if (enableSpecular and not unlit) then
			(
				doColorMultiplyMapOnDelegate &delegate.refl_color_map val specularColor
			)
		)

		on unlit set val do
		(
			-- Ignore param change on loading, since all the required params that we change for unlit 
			-- will be reset anyways depending on the paramblock parameter loading order.
			-- If unlit is enabled it will be applied in the post load handler.
			if (loading == false) do
			(
				enableUnlitMaterialParams val
			)
		)
	)

	rollout params_pbrnext ~GLTF_MATERIAL_MTLPARS~
	(
		-- Base Color
		label lblBaseColor ~GLTF_MATERIAL_BASECOL~ offset:[0,heightIndent] align:#left across:3
		colorpicker cpBaseColor "" color:white fieldWidth:colPickerWidth offset:[colPickerIndent3Cols,0] align:#left tooltip:~GLTF_MATERIAL_TT_BASECOLOR~
		mapbutton btnBaseColorMap "" align:#right width:mapBtnWidth

		-- Alpha Mode
		label lblAlphaMode ~GLTF_MATERIAL_ALPHA_MODE~ offset:[0,heightIndent] align:#left across:2
		dropdownlist cbAlphaMode "" items:#(~GLTF_MATERIAL_ALPHA_MODE_OPAQUE~, ~GLTF_MATERIAL_ALPHA_MODE_MASK~, ~GLTF_MATERIAL_ALPHA_MODE_BLEND~) width:(mapBtnWidth/2) align:#left tooltip:~GLTF_MATERIAL_TT_ALPHA_MODE~
		-- Alpha Map
		label lblAlphaMap ~GLTF_MATERIAL_ALPHA_MAP~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:2
		mapbutton btnAlphaMap "" align:#right offset:[-2,0] width:mapBtnWidth
		-- Alpha Cutoff
		label lblAlphaCutoff ~GLTF_MATERIAL_ALPHA_MODE_CUTOFF~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:2
		spinner spnAlphaCutoff "" range:[0.0,1.0,0.5] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent2Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_ALPHA_CUTOFF~

		-- Metalness
		label lblMetalness ~GLTF_MATERIAL_METALNESS~ offset:[0,heightIndent] align:#left across:3
		spinner spnMetalness "" range:[0.0,1.0,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_METALNESS~
		mapbutton btnMetalnessMap "" align:#right width:mapBtnWidth

		-- Roughness
		label lblRoughness ~GLTF_MATERIAL_ROUGHNESS~ offset:[0,heightIndent] align:#left across:3
		spinner spnRoughness "" range:[0.0,1.0,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_ROUGHNESS~
		mapbutton btnRoughnessMap "" align:#right width:mapBtnWidth

		-- Normal Map
		label lblNormalMap ~GLTF_MATERIAL_NORMAL_MAP~ offset:[0,heightIndent] align:#left across:3
		spinner spnNormal "" range:[-20.0,20.0,1.0] scale:0.01 fieldwidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_NORMAL~
		mapbutton btnNormalMap "" width:mapBtnWidth align:#right

		-- Ambient Occlusion
		label lblAmbientOcclusion ~GLTF_MATERIAL_AO~ offset:[0,heightIndent] align:#left across:3
		spinner spnAmbientOcclusion "" range:[0.0,1.0,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_AO~
		mapbutton btnAmbientOcclusionMap "" width:mapBtnWidth align:#right 

		-- Emission
		label lblEmission ~GLTF_MATERIAL_EMISSION~ offset:[0,heightIndent] align:#left across:3
		colorpicker cpEmissionColor "" color:black fieldWidth:colPickerWidth offset:[colPickerIndent3Cols,0] align:#left tooltip:~GLTF_MATERIAL_TT_EMISSION~
		mapbutton btnEmissionMap "" align:#right width:mapBtnWidth tooltip:~GLTF_MATERIAL_TT_EMISSION~

		-- DoubleSided
		checkbox cbDoubleSided ~GLTF_MATERIAL_DOUBLESIDED~ offset:[checkboxIndentLvl1,0] tooltip:~GLTF_MATERIAL_TT_DOUBLE_SIDED~

		fn UpdateBaseColorGroupUI mode = 
		(
			lblAlphaCutoff.enabled= (mode == 2)
			spnAlphaCutoff.enabled= (mode == 2)
			lblAlphaMap.enabled= (mode >= 2)
			btnAlphaMap.enabled= (mode >= 2)
		)

		on cbAlphaMode selected i do 
		(
			UpdateBaseColorGroupUI i
		)

		fn UpdateMetalnessGroupUI enable = 
		(
			lblMetalness.enabled = enable
			spnMetalness.enabled = enable
			btnMetalnessMap.enabled = enable
		)

		fn UpdateRoughnessGroupUI enable = 
		(
			lblRoughness.enabled = enable
			spnRoughness.enabled = enable
			btnRoughnessMap.enabled = enable
		)

		fn UpdateNormalGroupUI enable = 
		(
			lblNormalMap.enabled = enable
			spnNormal.enabled = enable
			btnNormalMap.enabled = enable
		)

		fn UpdateOcclusionGroupUI enable = 
		(
			lblAmbientOcclusion.enabled = enable
			spnAmbientOcclusion.enabled = enable
			btnAmbientOcclusionMap.enabled = enable
		)

		fn UpdateEmissionGroupUI enable = 
		(
			lblEmission.enabled = enable
			cpEmissionColor.enabled = enable
			btnEmissionMap.enabled = enable
		)

		on params_pbrnext open do
		(
			UpdateBaseColorGroupUI alphaMode
			UpdateCutoutMap alphaMode alphaMap alphaCutoff
			
			local enable = (not unlit)
			UpdateMetalnessGroupUI enable
			UpdateRoughnessGroupUI enable
			UpdateNormalGroupUI enable
			UpdateOcclusionGroupUI enable
			UpdateEmissionGroupUI enable
		)
	)

	rollout params_gltfext ~GLTF_EXTENSIONS_MTLPARS~
	(
		-- UNLIT EXTENSION -------------------------------------------------------------------------------------------
		checkbox cbUnlitMaterials ~GLTF_MATERIAL_ENABLE_UNLIT~ height:minCtrlHeight offset:[checkboxIndentLvl1,0] tooltip:~GLTF_MATERIAL_TT_UNLIT~

		-- CLEAR COAT EXTENSION -------------------------------------------------------------------------------------
		-- Coating
		checkbox cbEnableClearcoat ~GLTF_MATERIAL_ENABLE_CLEARCOAT~ offset:[checkboxIndentLvl1,0] across:3 tooltip:~GLTF_MATERIAL_TT_CLEARCOAT~
		spinner spnClearcoatFactor "" range:[0.0,1.0,1.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_CLEARCOAT~
		mapbutton btnClearcoatMap "" align:#right width:mapBtnWidth 
		-- Roughness
		label lblClearcoatRoughness ~GLTF_MATERIAL_CLEARCOAT_ROUGHNESS~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:3
		spinner spnClearcoatRoughness "" range:[0.0,1.0,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_CLEARCOAT_ROUGHNESS~
		mapbutton btnClearcoatRoughnessMap "" align:#right width:mapBtnWidth
		-- Normal
		label lblClearcoatNormalMap ~GLTF_MATERIAL_CLEARCOAT_NORMAL_MAP~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:3
		spinner spnClearcoatNormal "" range:[-20.0,20.0,1.0] scale:0.01 fieldwidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_CLEARCOAT_NORMAL~
		mapbutton btnClearcoatNormalMap "" width:mapBtnWidth align:#right 

		-- SHEEN EXTENSION -------------------------------------------------------------------------------------------
		-- Color
		checkbox cbEnableSheen ~GLTF_MATERIAL_ENABLE_SHEEN~ offset:[checkboxIndentLvl1,0] across:3 tooltip:~GLTF_MATERIAL_TT_SHEEN_COLOR~
		colorpicker cpSheenColor "" color:white fieldWidth:colPickerWidth offset:[colPickerIndent3Cols,0] align:#left tooltip:~GLTF_MATERIAL_TT_SHEEN_COLOR~
		mapbutton btnSheenColorMap "" align:#right width:mapBtnWidth
		-- Roughness
		label lblSheenRoughness ~GLTF_MATERIAL_SHEEN_ROUGHNESS~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:3
		spinner spnSheenRoughness "" range:[0.0,1.0,1.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_SHEEN_ROUGHNESS~
		mapbutton btnSheenRoughnessMap "" align:#right width:mapBtnWidth

		-- SPECULAR EXTENSION -------------------------------------------------------------------------------------------
		-- Strength of Reflections
		checkbox cbEnableSpecular ~GLTF_MATERIAL_ENABLE_SPECULAR~ offset:[checkboxIndentLvl1,0] across:3 tooltip:~GLTF_MATERIAL_TT_SPECULAR~
		spinner spnSpecularFactor "" range:[0.0,1.0,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_SPECULAR~
		mapbutton btnSpecularMap "" align:#right width:mapBtnWidth 
		-- Color
		label lblSpecularColor ~GLTF_MATERIAL_SPECULAR_COLOR~ offset:[paramLabelIndentLvl2,heightIndent] align:#left across:3
		colorpicker cpSpecularColor "" color:white fieldWidth:colPickerWidth offset:[colPickerIndent3Cols,0] align:#left tooltip:~GLTF_MATERIAL_TT_SPECULAR_COLOR~
		mapbutton btnSpecularColorMap "" align:#right width:mapBtnWidth

		-- TRANSMISSION EXTENSION -------------------------------------------------------------------------------------------
		checkbox cbEnableTransmission ~GLTF_MATERIAL_ENABLE_TRANSMISSION~ offset:[checkboxIndentLvl1,0] across:3 tooltip:~GLTF_MATERIAL_TT_TRANSMISSION~
		spinner spnTransmissionFactor "" range:[0.0,1.0,0.0] scale:0.01  fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_TRANSMISSION~
		mapbutton btnTransmissionMap "" align:#right width:mapBtnWidth 

		-- VOLUME EXTENSION -------------------------------------------------------------------------------------------
		-- Thickness
		checkbox cbEnableVolume ~GLTF_MATERIAL_ENABLE_VOLUME~ height:minCtrlHeight offset:[checkboxIndentLvl2,0] tooltip:~GLTF_MATERIAL_TT_ENABLE_VOLUME~
		label lblVolumeThickness ~GLTF_MATERIAL_VOLUME_THICKNESS~ offset:[paramLabelIndentLvl3,heightIndent] align:#left across:3
		spinner spnVolumeThickness "" range:[0.0,1e9,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent3Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_VOLUME_THICKNESS~
		mapbutton btnVolumeThicknessMap "" align:#right width:mapBtnWidth
		-- Distance
		label lblVolumeDistance ~GLTF_MATERIAL_VOLUME_DISTANCE~ offset:[paramLabelIndentLvl3,heightIndent] align:#left across:2
		spinner spnVolumeDistance "" range:[0.0,1e9,0.0] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent2Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_VOLUME_DISTANCE~
		-- Color
		label lblVolumeColor ~GLTF_MATERIAL_VOLUME_COLOR~ offset:[paramLabelIndentLvl3,0] align:#left across:2
		colorpicker cpVolumeColor "" color:white fieldWidth:colPickerWidth offset:[colPickerIndent2Cols,0] align:#left tooltip:~GLTF_MATERIAL_TT_VOLUME_COLOR~

		-- IOR EXTENSION -------------------------------------------------------------------------------------------
		checkbox cbEnableIOR ~GLTF_MATERIAL_ENABLE_IOR~ offset:[checkboxIndentLvl1,0] across:2 tooltip:~GLTF_MATERIAL_TT_IOR~
		spinner spnIOR "" range:[1.0,50.0,1.5] scale:0.01 fieldWidth:spinnerWidth offset:[spinnerIndent2Cols,heightIndent] align:#left tooltip:~GLTF_MATERIAL_TT_IOR~

		fn UpdateClearcoatGroupUI enable = 
		(
			spnClearcoatFactor.enabled = enable
			btnClearcoatMap.enabled = enable
			lblClearcoatRoughness.enabled = enable
			spnClearcoatRoughness.enabled = enable
			btnClearcoatRoughnessMap.enabled = enable
			lblClearcoatNormalMap.enabled = enable
			spnClearcoatNormal.enabled = enable
			btnClearcoatNormalMap.enabled = enable
		)

		on cbEnableClearcoat changed checked do 
		(
			UpdateClearcoatGroupUI checked
		)

		fn UpdateSheenGroupUI enable = 
		(
			cpSheenColor.enabled = enable
			btnSheenColorMap.enabled = enable
			lblSheenRoughness.enabled = enable
			spnSheenRoughness.enabled = enable
			btnSheenRoughnessMap.enabled = enable
		)

		on cbEnableSheen changed checked do 
		(
			UpdateSheenGroupUI checked
		)

		fn UpdateVolumeGroupUI enable = 
		(
			lblVolumeThickness.enabled = enable
			spnVolumeThickness.enabled = enable
			btnVolumeThicknessMap.enabled = enable
			lblVolumeDistance.enabled = enable
			spnVolumeDistance.enabled = enable
			lblVolumeColor.enabled = enable
			cpVolumeColor.enabled = enable
		)

		on cbEnableVolume changed checked do 
		(
			UpdateVolumeGroupUI checked
		)

		fn UpdateIORGroupUI enable = 
		(
			spnIOR.enabled = enable
		)

		on cbEnableIOR changed checked do 
		(
			UpdateIORGroupUI checked
		)

		fn UpdateTransmissionGroupUI enable = 
		(
			spnTransmissionFactor.enabled = enable
			btnTransmissionMap.enabled = enable
			
			cbEnableVolume.enabled = enable
			UpdateVolumeGroupUI (enable and enableVolume)
		)

		on cbEnableTransmission changed checked do
		(
			UpdateTransmissionGroupUI checked
		)

		fn UpdateSpecularGroupUI enable = 
		(
			spnSpecularFactor.enabled = enable
			btnSpecularMap.enabled = enable
			lblSpecularColor.enabled = enable
			cpSpecularColor.enabled = enable
			btnSpecularColorMap.enabled = enable
		)

		on cbEnableSpecular changed checked do 
		(
			UpdateSpecularGroupUI checked
		)

		on params_gltfext open do
		(
			local enable = (not unlit)
			cbEnableClearcoat.enabled = enable
			UpdateClearcoatGroupUI (enable and enableClearcoat)
			cbEnableSheen.enabled = enable
			UpdateSheenGroupUI (enable and enableSheen)
			cbEnableSpecular.enabled = enable
			UpdateSpecularGroupUI (enable and enableSpecular)
			cbEnableTransmission.enabled = enable
			UpdateTransmissionGroupUI (enable and enableTransmission)
			cbEnableIOR.enabled = enable
			UpdateIORGroupUI (enable and enableIndexOfRefraction)
		)
	)

	on postLoad do
	(
		-- Re-apply unlit after loading all parameters.
		if this.unlit do
		(
			enableUnlitMaterialParams true
		)
	)
)

-------BEGIN-SIGNATURE-----
-- cgkAADCCCW4GCSqGSIb3DQEHAqCCCV8wgglbAgEBMQ8wDQYJKoZIhvcNAQELBQAw
-- CwYJKoZIhvcNAQcBoIIHCjCCBwYwggTuoAMCAQICEAPoXQ8zvyfgRZ+u8YJGR4Uw
-- DQYJKoZIhvcNAQELBQAwaTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDkRpZ2lDZXJ0
-- LCBJbmMuMUEwPwYDVQQDEzhEaWdpQ2VydCBUcnVzdGVkIEc0IENvZGUgU2lnbmlu
-- ZyBSU0E0MDk2IFNIQTM4NCAyMDIxIENBMTAeFw0yMjA3MjcwMDAwMDBaFw0yMzA4
-- MTkyMzU5NTlaMIGKMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTET
-- MBEGA1UEBxMKU2FuIFJhZmFlbDEXMBUGA1UEChMOQXV0b2Rlc2ssIEluYy4xHzAd
-- BgNVBAsTFkRlc2lnbiBTb2x1dGlvbnMgR3JvdXAxFzAVBgNVBAMTDkF1dG9kZXNr
-- LCBJbmMuMIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAwDa9P4A2kzCY
-- kkQvhanUbJJF3K3fWIBjDvAcs/TqTAzYxqHbIW9Oi57R+NLNnB0m+GKwL7Vya0wb
-- UhZvEC8VN5Nw8ZjEDcevdrcv399QQeh5OQaVURgr5JsaSP8u2mdCfvrcnIDBntrf
-- hqGcQT2nioZ1H/PqUUcbOfEXlYMJa+SKm3yWvHkQwihiGWSP13RDDgSJlU+DebsT
-- zv+C9IpPLad8nSpuSA1vCywhzgzok+hDF9H/WJAl3Fv6WQvmj/vH6/8Nb+v5IE/K
-- JvAn+Er5un7fdAXhB+GXHH/bp4jXzq7wUxSxbamDDvpfS3OEN4BFoWgQ5t9eILSp
-- LecHsx9cFyLSbRP1kHBpsUib0lSItjgOAK3tFoE94DBnCWsfUsn8kYqWfcSlAV3l
-- G/T3OhPzHvPGAR2a84Mz71ywuP1j47RsPq2YndMlrxDZV+bGHefT717hDNc1lG+p
-- /Xnr1tdiqw+FHFI7aWIozEJ1zJB6wiXx7mgrQdB1LtNJk7pPOpYdAgMBAAGjggIG
-- MIICAjAfBgNVHSMEGDAWgBRoN+Drtjv4XxGG+/5hewiIZfROQjAdBgNVHQ4EFgQU
-- EF/sqJMBw1FZMj7ZWWrfrXY80GYwDgYDVR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoG
-- CCsGAQUFBwMDMIG1BgNVHR8Ega0wgaowU6BRoE+GTWh0dHA6Ly9jcmwzLmRpZ2lj
-- ZXJ0LmNvbS9EaWdpQ2VydFRydXN0ZWRHNENvZGVTaWduaW5nUlNBNDA5NlNIQTM4
-- NDIwMjFDQTEuY3JsMFOgUaBPhk1odHRwOi8vY3JsNC5kaWdpY2VydC5jb20vRGln
-- aUNlcnRUcnVzdGVkRzRDb2RlU2lnbmluZ1JTQTQwOTZTSEEzODQyMDIxQ0ExLmNy
-- bDA+BgNVHSAENzA1MDMGBmeBDAEEATApMCcGCCsGAQUFBwIBFhtodHRwOi8vd3d3
-- LmRpZ2ljZXJ0LmNvbS9DUFMwgZQGCCsGAQUFBwEBBIGHMIGEMCQGCCsGAQUFBzAB
-- hhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wXAYIKwYBBQUHMAKGUGh0dHA6Ly9j
-- YWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFRydXN0ZWRHNENvZGVTaWduaW5n
-- UlNBNDA5NlNIQTM4NDIwMjFDQTEuY3J0MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcN
-- AQELBQADggIBALEgKMMNJ9H5FKWijL5VSvloTQT20Cnln2tk7X3Q8o9b/Ba4qHBf
-- TAPfbF/OaW7k4SZNjHYKnZ3LnBCJFBZTegWigmRsKYwBD92Ot/x/nh2ZgZdwkI0k
-- UbONNkMx3Fo2OR/nxfM8eaevKj+P+QQEtabtDUXAD5e3tspdvCrZWdUk019N8D1c
-- /vLqKIXlPAM8a+1NfKNksZBgr9b3HRr9issohBniAJdE97OgF+2xBnGp72WEe9Uj
-- ZGiGOkGczbCDACjPcBrJqlWrOqJzSFgIzBjcTwBozRsKmGGme5lZ9I8LShBfyz3b
-- XMWL6lo0V5KAZilCLUiFYO3fFcElWLwL8csSD5oZn1zJZs4ZeFzuv1NiS/q7/UlP
-- W4KgAFrnJfRZYJ6OfTAhh4WNmgRXV88L8Fn54huczOnndBePsBE+IkVBT+pe5QwI
-- Dlt0K3EHlsYTE9YBcdXk7+JpWMm3q8etEICpI8UYGUbxhlbWpvb7kIAOyPXj62xG
-- uXSznSL/CSaN/3LoAOWeFnPdRiT1pHx6TXf66XnAtldtAfiW2MjRVhd8QGMYddoA
-- zFPOznYjiijwo+Dnckt0imDhziF6UlpLsY9qn/ZiHihWlJs/FfjWln3iCpG8UJzc
-- ALUxiX1dd/EIj5TuREOmVS4yvHrFhdJnOrsboWb54ZCzFu/23FK43IN1MYICKDCC
-- AiQCAQEwfTBpMQswCQYDVQQGEwJVUzEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4x
-- QTA/BgNVBAMTOERpZ2lDZXJ0IFRydXN0ZWQgRzQgQ29kZSBTaWduaW5nIFJTQTQw
-- OTYgU0hBMzg0IDIwMjEgQ0ExAhAD6F0PM78n4EWfrvGCRkeFMA0GCSqGSIb3DQEB
-- CwUAMA0GCSqGSIb3DQEBAQUABIIBgJndzR0hYtLensVvik2722T9DLNQLrwRp6/7
-- Cd3SirYApAcQc5IBmTNptHY1AvPgedz5Rfo2B2yU71Vq0CUgSoWCvaINgYyX4UAW
-- z+3s7tzqTjh+HMDd04jvEFqwR64Bq1x1WQaW3FrqvU7bsefGNVWZ8c8Gpgg9FkFn
-- uE8HxSGmEE59E2GHgCf5hzB2iGpuXu8SeCRELgkMTy2jtgyZ2JPyPquSmK8Dcg41
-- gnrB5dBihTLRD+nDD8XsrOSiTkCkw28OrsakExTIZ3CMP4KRuTIZJuQggRc9hQ2V
-- +0WPyg+Yky6/0Xo8w2/cvCWNp+7muW1DYLxqS5OA5cMKRJP/yBDQaGmQZZria5M+
-- TYvsN72uCcHc0AG24WuHpkpdzNuvXn/ujnZr/kClDPdXVBezmV74uGe4ut1eNNoi
-- ovbl2fRUINbD9kli1mzI3x7Mswdmy4vRS+GHPZFQj018emZLInhx+zxcodI1T5CM
-- XP8kv6QoGWamZmeCwHeo3J4V/5xDqA==
-- -----END-SIGNATURE-----